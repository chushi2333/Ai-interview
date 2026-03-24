package com.chushi.aiinterview.commons.utils.identifier;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花ID生成器（CAS无锁优化版）
 * 基于 Twitter 的 Snowflake 算法实现分布式唯一 ID 生成
 * <p>
 * ID结构（64位）：
 * <pre>
 * +----------+----------------+------------+------------+
 * | 1bit符号 | 41bit时间戳     | 10bit机器ID | 12bit序列号 |
 * +----------+----------------+------------+------------+
 * </pre>
 * <ul>
 *   <li>时间戳：毫秒级，可使用约69年</li>
 *   <li>机器ID：支持0-1023共1024个节点</li>
 *   <li>序列号：每毫秒最多生成4096个ID</li>
 * </ul>
 * <p>
 * 性能优化：使用 CAS (Compare-And-Swap) 替代 synchronized 锁，
 * 实现无锁并发，大幅提升高并发场景下的吞吐量
 */
@Slf4j
public class SnowflakeIdGenerator implements IdGenerator<Long>{
    /**
     * 基准时间戳 (2025-10-01 00:00:00 UTC)
     */
    private static final long EPOCH = 1759248000000L;

    /**
     * 各部分位数
     */
    private static final long MACHINE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 各部分最大值
     */
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_ID_BITS); // 1023
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);     // 4095

    /**
     * 位移量
     */
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;

    /**
     * 时钟回拨最大容忍时间（毫秒）
     */
    private static final long MAX_BACKWARD_MS = 5L;

    /**
     * CAS 最大自旋次数，避免无限自旋
     */
    private static final int MAX_SPIN_COUNT = 10000;

    /**
     * 状态编码说明：
     * 将时间戳和序列号编码到一个 long 中
     * 高 52 位：时间戳（足够存储很长时间）
     * 低 12 位：序列号（0-4095）
     */
    private static final int STATE_SEQUENCE_BITS = 12;
    private static final long STATE_SEQUENCE_MASK = ~(-1L << STATE_SEQUENCE_BITS);

    /**
     * 机器ID（通过配置注入，范围：0-1023）
     */
    @Getter
    private final long machineId;

    /**
     * 原子状态变量，使用 CAS 操作
     * 编码格式：[52位时间戳][12位序列号]
     */
    private final AtomicLong lastState = new AtomicLong(0L);

    /**
     * 构造雪花ID生成器
     *
     * @param machineId 机器ID，取值范围 [0, 1023]
     * @throws IllegalArgumentException 如果机器ID超出有效范围
     */
    public SnowflakeIdGenerator(long machineId) {
        if (machineId < 0 || machineId > MAX_MACHINE_ID) {
            throw new IllegalArgumentException(
                    String.format("Machine ID must be between [0, %d], current value: %d", MAX_MACHINE_ID, machineId)
            );
        }
        this.machineId = machineId;
        log.info("Snowflake ID generator initialized (CAS mode), machine ID: {}, epoch: {}",
                machineId,
                LocalDateTime.ofInstant(Instant.ofEpochMilli(EPOCH), ZoneId.systemDefault()));
    }

    /**
     * 解析雪花ID，提取其中的时间戳、机器ID和序列号
     *
     * @param snowflakeId 雪花ID
     * @return ID信息对象
     */
    public static SnowflakeIdInfo parseId(long snowflakeId) {
        long timestamp = ((snowflakeId >> TIMESTAMP_SHIFT) & 0x1FFFFFFFFFFL) + EPOCH;
        long machineId = (snowflakeId >> MACHINE_ID_SHIFT) & MAX_MACHINE_ID;
        long sequence = snowflakeId & MAX_SEQUENCE;

        return new SnowflakeIdInfo(snowflakeId, timestamp, machineId, sequence);
    }

    /**
     * 验证雪花ID是否有效
     *
     * @param snowflakeId 雪花ID
     * @return 是否有效
     */
    public static boolean isValidId(long snowflakeId) {
        if (snowflakeId <= 0) {
            return false;
        }

        try {
            SnowflakeIdInfo info = parseId(snowflakeId);
            return info.timestamp() >= EPOCH
                    && info.machineId() >= 0
                    && info.machineId() <= MAX_MACHINE_ID
                    && info.sequence() >= 0
                    && info.sequence() <= MAX_SEQUENCE;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成下一个唯一ID（无锁线程安全）
     * <p>
     * 使用 CAS 自旋方式实现无锁并发，相比 synchronized 性能更优
     *
     * @return 64位雪花ID
     * @throws RuntimeException 当发生严重的时钟回拨或自旋次数过多时
     */
    @Override
    public Long nextId() {
        int spinCount = 0;

        while (true) {
            // 放置无限自旋。
            if (++spinCount > MAX_SPIN_COUNT) {
                log.error("CAS spin count exceeded maximum limit: {}", MAX_SPIN_COUNT);
                throw new RuntimeException("Failed to generate ID: too many concurrent requests");
            }

            // 读取当前状态
            long currentState = lastState.get();
            long lastTimestamp = decodeTimestamp(currentState);
            long lastSequence = decodeSequence(currentState);

            long timestamp = getCurrentTimestamp();

            // 时钟回拨处理
            if (timestamp < lastTimestamp) {
                timestamp = handleClockBackwardWithCAS(timestamp, lastTimestamp);
                if (timestamp < 0) {
                    // 严重回拨，已抛出异常
                    continue;
                }
            }

            long newSequence;
            long newTimestamp;

            // 同一毫秒内生成多个ID
            if (timestamp == lastTimestamp) {
                newSequence = (lastSequence + 1) & MAX_SEQUENCE;

                // 序列号溢出，等待下一毫秒
                if (newSequence == 0L) {
                    timestamp = waitNextMillis(lastTimestamp);
                }
                newTimestamp = timestamp;
            } else {
                // 不同毫秒，序列号重置为0
                newTimestamp = timestamp;
                newSequence = 0L;
            }

            // 编码新状态
            long newState = encodeState(newTimestamp, newSequence);

            // CAS 更新状态
            if (lastState.compareAndSet(currentState, newState)) {
                // CAS 成功，组装并返回ID
                return assembleId(newTimestamp, newSequence);
            }

            // CAS 失败，自旋重试
            // 在高并发场景下，适当让出CPU时间片
            if (spinCount % 100 == 0) {
                Thread.yield();
            }
        }
    }

    /**
     * 将时间戳和序列号编码到一个 long 中
     *
     * @param timestamp 时间戳
     * @param sequence  序列号
     * @return 编码后的状态值
     */
    private long encodeState(long timestamp, long sequence) {
        return (timestamp << STATE_SEQUENCE_BITS) | (sequence & STATE_SEQUENCE_MASK);
    }

    /**
     * 从状态值中解码时间戳
     *
     * @param state 状态值
     * @return 时间戳
     */
    private long decodeTimestamp(long state) {
        return state >> STATE_SEQUENCE_BITS;
    }

    /**
     * 从状态值中解码序列号
     *
     * @param state 状态值
     * @return 序列号
     */
    private long decodeSequence(long state) {
        return state & STATE_SEQUENCE_MASK;
    }

    /**
     * 获取当前时间戳
     *
     * @return 当前毫秒时间戳
     */
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 处理时钟回拨（CAS 模式）
     * <p>
     * 注意：在 CAS 模式下，等待操作不会阻塞其他线程
     *
     * @param currentTimestamp 当前时间戳
     * @param lastTimestamp    上次时间戳
     * @return 处理后的时间戳，如果发生严重回拨则返回负数
     * @throws RuntimeException 当时钟回拨超过容忍时间时
     */
    private long handleClockBackwardWithCAS(long currentTimestamp, long lastTimestamp) {
        long offset = lastTimestamp - currentTimestamp;

        if (offset <= MAX_BACKWARD_MS) {
            // 小幅度回拨，等待时钟追上
            log.warn("Clock moved backwards by {} ms, waiting for clock synchronization...", offset);
            try {
                Thread.sleep(offset);
                return getCurrentTimestamp();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for clock synchronization", e);
            }
        } else {
            // 大幅度回拨，抛出异常
            log.error("Severe clock backwards detected! Offset: {} ms, current: {}, last: {}",
                    offset, currentTimestamp, lastTimestamp);
            throw new RuntimeException(
                    String.format("Clock moved backwards, refusing to generate ID. Offset: %d ms", offset)
            );
        }
    }

    /**
     * 等待直到下一毫秒
     *
     * @param lastTimestamp 上次时间戳
     * @return 新的时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }

    /**
     * 组装ID
     *
     * @param timestamp 当前时间戳
     * @param sequence  序列号
     * @return 组装后的雪花ID
     */
    private long assembleId(long timestamp, long sequence) {
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (machineId << MACHINE_ID_SHIFT)
                | sequence;
    }

    /**
     * 雪花ID信息
     *
     * @param id        完整的雪花ID
     * @param timestamp 时间戳（毫秒）
     * @param machineId 机器ID
     * @param sequence  序列号
     */
    public record SnowflakeIdInfo(
            long id,
            long timestamp,
            long machineId,
            long sequence
    ) {
        /**
         * 获取生成时间
         *
         * @return LocalDateTime对象
         */
        public LocalDateTime getGenerateTime() {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );
        }

        @Override
        public @NotNull String toString() {
            return String.format(
                    "SnowflakeIdInfo{id=%d, timestamp=%d, time=%s, machineId=%d, sequence=%d}",
                    id, timestamp, getGenerateTime(), machineId, sequence
            );
        }
    }
}
