package com.chushi.aiinterview.commons.utils.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 缓存配置属性类
 * <p>
 * 通过 Spring Boot 的 {@code @ConfigurationProperties} 机制从配置文件中读取缓存相关参数。
 * 所有属性都提供了合理的默认值，可根据实际业务场景在 {@code redis-cache.yml} 中覆盖。
 * </p>
 * <p>
 * 配置示例（redis-cache.yml）:
 * <pre>
 * redis-cache:
 *   physical-expire-delay: 120        # 物理过期延迟 120 秒
 *   max-random-expire-delay: 60       # 最大随机延迟 60 秒
 *   max-try-lock-times: 10            # 最多尝试获取锁 10 次
 *   try-lock-sleep-time: 100          # 获取锁失败后休眠 100 毫秒
 *   lock-wait-time: 15                # 等待锁超时时间 15 秒
 *   lock-lease-time: 30               # 锁租约时间 30 秒
 * </pre>
 * </p>
 * <p>
 * 性能调优建议:
 * <ul>
 *   <li>热点数据：增大 {@code physicalExpireDelay}，延长缓存有效期</li>
 *   <li>高并发场景：增大 {@code maxTryLockTimes} 和 {@code lockWaitTime}，避免获取锁失败</li>
 *   <li>大规模缓存：增大 {@code maxRandomExpireDelay}，分散过期时间防止雪崩</li>
 *   <li>低延迟要求：减小 {@code tryLockSleepTime}，加快锁重试速度</li>
 * </ul>
 * </p>
 *
 * @author ChuShi
 * @see BaseRedisCacheTemplate
 * @see PreconfiguredRedisCacheTemplate
 */
@Component
@Data
@ConfigurationProperties(prefix = "redis-cache")
public class RedisCacheProperties {
    /**
     * 分布式锁租约时间（秒）
     * <p>
     * 使用 Redisson 分布式锁时的 leaseTime 参数，表示锁的最大持有时间。
     * 到期后锁会自动释放，防止因程序崩溃或异常导致的死锁。
     * </p>
     * <p>
     * 默认值：{@code 30} 秒
     * </p>
     * <p>
     * 设置原则：
     * <ul>
     *   <li>应大于从数据库加载数据的最长耗时，避免锁提前释放</li>
     *   <li>不宜过长，否则在异常情况下会长时间占用锁</li>
     *   <li>通常设置为正常操作耗时的 3-5 倍</li>
     * </ul>
     * </p>
     * <p>
     * 注意：Redisson 支持看门狗（WatchDog）机制，如果不设置 leaseTime，
     * 锁会自动续期，但为了安全起见，建议显式设置租约时间。
     * </p>
     */
    private final Long lockLeaseTime = 30L;

    /**
     * 物理过期时间延迟（秒）
     * <p>
     * 在逻辑过期时间的基础上额外增加的时间，用于计算 Redis 键的 TTL（物理过期时间）。
     * 这样可以确保在逻辑过期后仍有一段时间窗口用于异步刷新缓存，避免缓存完全消失。
     * </p>
     * <p>
     * 默认值：{@code 60} 秒
     * </p>
     * <p>
     * 物理过期时间计算公式：
     * <pre>
     * 物理 TTL = 逻辑 TTL + physicalExpireDelay + [0, maxRandomExpireDelay)
     * </pre>
     * </p>
     * <p>
     * 使用场景：
     * <ul>
     *   <li>如果逻辑 TTL 为 300 秒，延迟 60 秒，则物理 TTL 至少为 360 秒</li>
     *   <li>在 300-360 秒之间，缓存逻辑上已过期但物理上仍存在，可触发异步刷新</li>
     *   <li>360 秒后如果仍未刷新，Redis 会自动清理该键（处理长期无人访问的数据）</li>
     * </ul>
     * </p>
     * <p>
     * 设置建议：
     * <ul>
     *   <li>高频访问数据：延迟可以设置较大（120-300 秒），保证足够的刷新时间</li>
     *   <li>低频访问数据：延迟可以设置较小（30-60 秒），避免占用过多内存</li>
     * </ul>
     * </p>
     */
    private Long physicalExpireDelay = 60L;

    /**
     * 最大随机过期延迟（秒）
     * <p>
     * 在物理过期时间上增加的随机延迟范围 {@code [0, maxRandomExpireDelay)}，
     * 用于防止缓存雪崩（大量缓存同时过期导致数据库瞬时压力过大）。
     * </p>
     * <p>
     * 默认值：{@code 60} 秒
     * </p>
     * <p>
     * 防雪崩原理：
     * 假设有 10000 个用户的缓存逻辑 TTL 都是 300 秒，如果没有随机延迟，
     * 它们会在同一时刻物理过期，导致数据库瞬间承受 10000 次查询。
     * 通过增加随机延迟，这 10000 个缓存会在 300-360 秒之间分散过期，
     * 平滑了数据库压力曲线。
     * </p>
     * <p>
     * 设置建议：
     * <ul>
     *   <li>缓存数量较多：增大随机延迟（60-300 秒），分散过期时间</li>
     *   <li>缓存数量较少：可以减小随机延迟（10-30 秒）</li>
     *   <li>建议设置为逻辑 TTL 的 10%-20%</li>
     * </ul>
     * </p>
     */
    private Long maxRandomExpireDelay = 60L;

    /**
     * 最大尝试获取锁次数
     * <p>
     * 当缓存未命中时，线程需要获取分布式锁才能从数据库加载数据。
     * 如果获取锁失败，会重试指定次数，超过此次数后抛出异常。
     * </p>
     * <p>
     * 默认值：{@code 5} 次
     * </p>
     * <p>
     * 重试机制：
     * <ul>
     *   <li>第一次尝试获取锁失败后，休眠 {@code tryLockSleepTime} 毫秒</li>
     *   <li>然后进行第二次尝试，依此类推</li>
     *   <li>如果 5 次尝试全部失败，说明锁竞争非常激烈或持锁线程出现问题</li>
     * </ul>
     * </p>
     * <p>
     * 设置建议：
     * <ul>
     *   <li>高并发场景：增大重试次数（10-20 次），提高获取锁成功率</li>
     *   <li>低并发场景：可以减小重试次数（3-5 次），快速失败</li>
     *   <li>总等待时间 = maxTryLockTimes × tryLockSleepTime，应小于接口超时时间</li>
     * </ul>
     * </p>
     */
    private Long maxTryLockTimes = 5L;

    /**
     * 获取锁失败后的休眠时间（毫秒）
     * <p>
     * 当尝试获取分布式锁失败后，线程会休眠指定时间再重试，避免忙等待浪费 CPU 资源。
     * </p>
     * <p>
     * 默认值：{@code 50} 毫秒
     * </p>
     * <p>
     * 设置原则：
     * <ul>
     *   <li>不宜过短：过短会导致频繁重试，增加 Redis 压力</li>
     *   <li>不宜过长：过长会导致请求响应延迟增加</li>
     *   <li>建议设置为正常数据库查询耗时的 1/10 左右</li>
     * </ul>
     * </p>
     * <p>
     * 使用场景：
     * <ul>
     *   <li>如果数据库查询耗时约 100-200 毫秒，休眠时间可设置为 50 毫秒</li>
     *   <li>如果数据库查询耗时约 500-1000 毫秒，休眠时间可设置为 100-200 毫秒</li>
     * </ul>
     * </p>
     */
    private Long tryLockSleepTime = 50L;

    /**
     * 分布式锁等待时间（秒）
     * <p>
     * 使用 Redisson 的 {@code tryLock(waitTime, leaseTime, TimeUnit)} 方法时的 waitTime 参数。
     * 表示线程愿意等待获取锁的最长时间，超时后返回 false。
     * </p>
     * <p>
     * 默认值：{@code 10} 秒
     * </p>
     * <p>
     * 与 maxTryLockTimes 的区别：
     * <ul>
     *   <li>{@code lockWaitTime}：单次 tryLock 调用的超时时间</li>
     *   <li>{@code maxTryLockTimes}：失败后重试的次数</li>
     *   <li>总等待时间 ≤ maxTryLockTimes × (lockWaitTime + tryLockSleepTime)</li>
     * </ul>
     * </p>
     * <p>
     * 设置建议：
     * <ul>
     *   <li>应小于接口超时时间，避免请求超时</li>
     *   <li>应大于正常数据库查询耗时，给持锁线程足够的执行时间</li>
     *   <li>建议设置为数据库查询耗时的 2-3 倍</li>
     * </ul>
     * </p>
     */
    private Long lockWaitTime = 10L;
}
