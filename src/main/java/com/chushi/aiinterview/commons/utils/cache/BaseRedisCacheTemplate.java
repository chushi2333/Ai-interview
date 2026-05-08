package com.chushi.aiinterview.commons.utils.cache;

import com.chushi.aiinterview.exceptions.CacheException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Redis 缓存基础模板类
 * <p>
 * 提供了完整的 Redis 缓存操作功能，采用多层防护机制确保系统的稳定性和性能：
 * </p>
 * <ul>
 *   <li><b>防缓存穿透</b>：通过缓存空值（null）来防止恶意请求不存在的数据导致数据库压力</li>
 *   <li><b>防缓存击穿</b>：使用 Redisson 分布式锁确保热点数据过期时只有一个线程从数据库加载</li>
 *   <li><b>防缓存雪崩</b>：通过随机过期延迟避免大量缓存同时失效</li>
 *   <li><b>逻辑过期机制</b>：在缓存数据中存储逻辑过期时间，当数据逻辑过期时异步刷新缓存，同时返回旧数据，保证服务可用性</li>
 *   <li><b>物理过期机制</b>：设置 Redis 键的 TTL，确保长期无人访问的数据会被自动清理</li>
 * </ul>
 * <p>
 * 缓存刷新策略：
 * <ol>
 *   <li>缓存命中且未逻辑过期：直接返回缓存数据</li>
 *   <li>缓存命中但已逻辑过期：返回旧数据的同时，异步刷新缓存（保证服务不中断）</li>
 *   <li>缓存未命中：通过分布式锁从数据源加载并缓存数据</li>
 * </ol>
 * </p>
 *
 * @param <K> 缓存键的类型，通常为 Long、String 等
 * @param <V> 缓存值的类型，业务对象类型
 * @author ChuShi
 * @see RedisCacheData
 * @see PreconfiguredRedisCacheTemplate
 */
public class BaseRedisCacheTemplate<K, V> {
    /**
     * 缓存值的类型
     */
    private final Class<V> valueType;

    /**
     * 缓存值的 JavaType
     * <p>
     * 通过 Jackson 的 TypeFactory 构造
     * </p>
     */
    private JavaType cacheType;

    /**
     * Jackson 对象映射器
     * <p>
     * 用于序列化和反序列化缓存数据。
     * 通过 Bean 注入，可配置自定义的序列化策略（如日期格式、null 处理等）。
     * </p>
     */
    @Resource
    private ObjectMapper redisCacheObjectMapper;

    /**
     * 缓存刷新专用线程池
     * <p>
     * 当缓存逻辑过期时，使用该线程池异步刷新缓存，避免阻塞主线程。
     * 线程池配置需要根据系统负载合理设置核心线程数和最大线程数。
     * </p>
     */
    @Resource(name = "redisCacheRefreshTaskExecutor")
    private ThreadPoolTaskExecutor refreshTaskExecutor;

    /**
     * Spring Redis 模板
     * <p>
     * 提供对 Redis 的基本操作，如 get、set、delete 等。
     * 使用 String 类型存储序列化后的 JSON 数据。
     * </p>
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Redisson 客户端
     * <p>
     * 提供分布式锁功能，用于防止缓存击穿。
     * 使用 Redisson 的锁机制确保在高并发场景下只有一个线程能够访问数据库。
     * </p>
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 构造函数
     * <p>
     * 由于 Java 泛型擦除，运行时无法获取泛型参数类型，因此需要显式传入 valueType。
     * 该类型用于在反序列化时构造正确的 JavaType，确保 JSON 能够正确转换为目标对象。
     * </p>
     *
     * @param valueType 缓存值的 Class 对象，用于反序列化时确定类型
     */
    public BaseRedisCacheTemplate(Class<V> valueType) {
        this.valueType = valueType;
    }

    @PostConstruct
    private void init() {
        // 构造 RedisCacheData<V> 的 JavaType，用于反序列化
        this.cacheType = redisCacheObjectMapper
                .getTypeFactory()
                .constructParametricType(RedisCacheData.class, this.valueType);
    }

    /**
     * 设置缓存
     * <p>
     * 将数据序列化后存储到 Redis 中，包含逻辑过期时间和物理过期时间双重保护。
     * </p>
     * <p>
     * 过期时间计算逻辑：
     * <ul>
     *   <li><b>逻辑过期时间</b>：当前时间 + logicalTTL，存储在缓存数据中</li>
     *   <li><b>物理过期时间</b>：logicalTTL + expireDelay + randomDelay，设置为 Redis 键的 TTL</li>
     *   <li><b>随机延迟</b>：0 到 maxRandomDelay 之间的随机值，防止缓存雪崩</li>
     * </ul>
     * </p>
     * <p>
     * 物理过期时间通常大于逻辑过期时间，这样在逻辑过期后仍有时间窗口进行异步刷新，
     * 避免在刷新期间缓存完全消失导致的击穿问题。
     * </p>
     *
     * @param keyPrefix      缓存键前缀，用于区分不同业务模块的缓存
     * @param id             缓存键的唯一标识符（如用户ID、商品ID等）
     * @param value          要缓存的值，可以为 null（用于防止缓存穿透）
     * @param logicalTTL     逻辑过期时间（秒），业务层面的有效期
     * @param expireDelay    物理过期延迟（秒），在逻辑过期时间基础上增加的额外时间
     * @param maxRandomDelay 最大随机过期延迟（秒），用于防止缓存雪崩
     * @throws CacheException 序列化失败或 Redis 操作失败时抛出
     */
    public void setCache(String keyPrefix, K id, V value, long logicalTTL, long expireDelay, long maxRandomDelay) {
        // 拼接完整的 Redis 键：{统一前缀}:{业务前缀}:{ID}{
        var fullKey = RedisCacheConstants.CACHE_KEY_PREFIX + keyPrefix + ":" + id;

        // 生成随机过期延迟，范围 [0, maxRandomDelay)，防止缓存雪崩
        var randomDelay = ThreadLocalRandom.current().nextLong(maxRandomDelay);

        // 构造 RedisCacheDate 对象，包装原始数据和逻辑过期时间
        var cacheData = new RedisCacheData<V>();

        // 计算逻辑过期时间戳（Unix 时间戳，秒级）
        var logicExpireTimestamp = Instant.now().plusSeconds(logicalTTL).getEpochSecond();
        // 计算实际物理过期时间：逻辑 TTL + 延迟 + 随机延迟
        var expireTime = logicalTTL + expireDelay + randomDelay;

        // 设置缓存数据和逻辑过期时间
        cacheData.setValue(value);
        cacheData.setExpireTime(logicExpireTimestamp);

        try {
            // 序列化为 JSON 字符串
            var cacheJson = redisCacheObjectMapper.writeValueAsString(cacheData);
            // 保存到 Redis，设置物理过期时间（TTL）
            this.stringRedisTemplate.opsForValue().set(fullKey, cacheJson, expireTime, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 捕获序列化异常或 Redis 操作异常，统一包装为 CacheException
            throw new CacheException(e);
        }
    }

    /**
     * 获取缓存
     * <p>
     * 从 Redis 中获取指定键的缓存数据并反序列化。
     * 返回的 {@link RedisCacheData} 对象包含实际数据和逻辑过期时间。
     * </p>
     * <p>
     * 注意：该方法只检查物理过期（Redis 的 TTL），不检查逻辑过期。
     * 逻辑过期的判断由调用方（如 queryById）负责。
     * </p>
     *
     * @param keyPrefix 缓存键前缀
     * @param id        缓存键的唯一标识符
     * @return 包含缓存数据的 Optional，如果缓存不存在或已物理过期则返回 {@link Optional#empty()}
     * @throws CacheException 反序列化失败时抛出
     */
    @SuppressWarnings("unchecked")
    public Optional<RedisCacheData<V>> getCache(String keyPrefix, K id) {
        // 拼接完整的 Redis 键
        var fullKey = RedisCacheConstants.CACHE_KEY_PREFIX + keyPrefix + ":" + id;

        try {
            // 从 Redis 获取 JSON 字符串
            var cacheJson = this.stringRedisTemplate.opsForValue().get(fullKey);
            if (cacheJson == null) {
                // 缓存不存在或已物理过期
                return Optional.empty();
            }
            // 反序列化为 RedisCacheData 对象
            var cacheData = (RedisCacheData<V>) redisCacheObjectMapper.readValue(cacheJson, cacheType);
            return Optional.of(cacheData);
        } catch (Exception e) {
            // 捕获反序列化异常，统一包装为 CacheException
            throw new CacheException(e);
        }
    }

    /**
     * 刷新缓存
     * <p>
     * 从数据源（通常是数据库）获取最新数据并更新缓存。
     * 支持缓存空值以防止缓存穿透。
     * </p>
     * <p>
     * 刷新逻辑：
     * <ol>
     *   <li>调用 queryMethod 从数据源获取数据</li>
     *   <li>如果数据不存在（Optional.empty），缓存 null 值，防止缓存穿透</li>
     *   <li>如果数据存在，更新缓存为最新数据</li>
     * </ol>
     * </p>
     *
     * @param keyPrefix      缓存键前缀
     * @param id             缓存键的唯一标识符
     * @param queryMethod    数据查询方法，接收 ID 返回 Optional 包装的数据
     * @param logicalTTL     逻辑过期时间（秒）
     * @param expireDelay    物理过期延迟（秒）
     * @param maxRandomDelay 最大随机过期延迟（秒）
     * @return 更新后的数据的 Optional，如果数据不存在则返回 {@link Optional#empty()}
     */
    public Optional<V> refreshCache(String keyPrefix, K id, Function<K, Optional<V>> queryMethod, long logicalTTL, long expireDelay, long maxRandomDelay) {
        // 从数据库原获取最新数据
        var value = queryMethod.apply(id);
        // 如果数据库不存在，缓存空对象放置缓存穿透
        // 控制也会设置 TTL， 避免占用过多内存
        if (value.isEmpty()) {
            this.setCache(keyPrefix, id, null, logicalTTL, expireDelay, maxRandomDelay);
            return Optional.empty();
        }
        // 更新缓存为最新数据
        this.setCache(keyPrefix, id, value.get(),  logicalTTL, expireDelay, maxRandomDelay);
        return value;
    }

    /**
     * 根据 ID 查询数据
     * <p>
     * 核心查询方法，整合了多级缓存策略和防护机制。
     * </p>
     * <p>
     * 查询流程：
     * <ol>
     *   <li><b>第一步：尝试从缓存获取</b>
     *     <ul>
     *       <li>缓存命中且未逻辑过期：直接返回缓存数据（最快路径）</li>
     *       <li>缓存命中但已逻辑过期：返回旧数据，同时提交异步任务刷新缓存（保证服务不中断）</li>
     *       <li>缓存值为 null：说明数据不存在（防缓存穿透），直接返回空</li>
     *     </ul>
     *   </li>
     *   <li><b>第二步：缓存未命中或已物理过期，从数据源加载</b>
     *     <ul>
     *       <li>尝试获取分布式锁（防缓存击穿）</li>
     *       <li>获取锁成功：Double Check 缓存，如果仍未命中则从数据源加载并缓存</li>
     *       <li>获取锁失败：等待一段时间后重试，最多重试 maxTryLockTimes 次</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     * <p>
     * 异步刷新机制：
     * 当缓存逻辑过期时，不会阻塞当前请求，而是立即返回旧数据，并在后台线程池中异步刷新缓存。
     * 这种设计确保了服务的高可用性，避免了因缓存刷新导致的请求延迟。
     * </p>
     *
     * @param keyPrefix        缓存键前缀
     * @param id               要查询的数据 ID
     * @param queryMethod      数据查询方法，从数据源获取数据
     * @param maxTryLockTimes  获取分布式锁的最大尝试次数，防止无限等待
     * @param tryLockSleepTime 获取锁失败后的休眠时间（毫秒），避免频繁重试
     * @param logicalTTL       逻辑过期时间（秒）
     * @param expireDelay      物理过期延迟（秒）
     * @param maxRandomDelay   最大随机过期延迟（秒）
     * @param lockWaitTime     尝试获取分布式锁的等待时间（秒），使用 Redisson 的 tryLock(waitTime, leaseTime)
     * @param lockLeaseTime    分布式锁的租约时间（秒），防止死锁，到期自动释放
     * @return 查询到的数据的 Optional，如果数据不存在则返回 {@link Optional#empty()}
     * @throws CacheException 获取锁超时或其他缓存操作失败时抛出
     */
    public Optional<V> queryById(String keyPrefix, K id, Function<K, Optional<V>> queryMethod, long maxTryLockTimes, long tryLockSleepTime, long logicalTTL, long expireDelay, long maxRandomDelay, long lockWaitTime, long lockLeaseTime) {
        // 构造分布式锁的键名
        var fullLockKey = RedisCacheConstants.CACHE_LOCK_KEY_PREFIX + keyPrefix + ":" + id;
        var lock = this.redissonClient.getLock(fullLockKey);

        // 第一步：检查缓存是否存在
        var optionalCachedData = this.getCache(keyPrefix, id);
        // 如果缓存已经存在且物理时间未过期（Redis TTL 未到期）
        if (optionalCachedData.isPresent()) {
            var cacheData = optionalCachedData.get();
            // 判断是否逻辑过期
            var logicExpireTimeInstant = Instant.ofEpochSecond(cacheData.getExpireTime());
            if (logicExpireTimeInstant.isBefore(Instant.now())) {
                // 逻辑已经过期：在新线程中异步更新缓存，不阻塞当前请求
                this.refreshTaskExecutor.submit(() -> {
                    try {
                        // 尝试获取分布式锁，避免多个线程同时刷新
                        if (lock.tryLock(lockWaitTime, lockLeaseTime, TimeUnit.SECONDS)) {
                            // 更新缓存为最新数据
                            this.refreshCache(keyPrefix, id, queryMethod, logicalTTL, expireDelay, maxRandomDelay);
                        }
                    } catch (Exception e) {
                        // 异步任务中的异常不影响主线程，记录日志或统一处理
                        throw new CacheException(e);
                    } finally {
                        // 确保锁被正确释放
                        if(lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                });
            }
            // 检查实际包装的数据是否为 null（防缓存穿透的空值）
            if (cacheData.getValue() == null) {
                return Optional.empty();
            }
            // 返回缓存数据（可能是就数据，保证服务可用）
            return Optional.of(cacheData.getValue());
        }

        // 第二步：缓存未命或物理过期，需要从数据源加载
        try {
            // 尝试获取分布式锁，防止缓存击穿
            for (var i = 0;i < maxTryLockTimes; ++i) {
                if (lock.tryLock(lockWaitTime, lockLeaseTime, TimeUnit.SECONDS)) {
                    // Double Check：在等待锁期间，可能已有其他线程刷新了缓存
                    // 这里不需要检查逻辑过期时间，因为刚刷新的缓存不太可能已经逻辑过期
                    // 除非 logicalTTL 设置得极短（这种情况下性能 overhead 不值得再检查一次）
                    optionalCachedData = this.getCache(keyPrefix, id);
                    if (optionalCachedData.isPresent()) {
                        var cachedData = optionalCachedData.get();
                        // 检查是否为空值缓存
                        if (cachedData.getValue() != null) {
                            return Optional.of(cachedData.getValue());
                        }
                        return Optional.empty();
                    }
                    // 缓存仍未命中，从数据源加载并刷新缓存
                    return this.refreshCache(keyPrefix, id, queryMethod, logicalTTL, expireDelay, maxRandomDelay);
                }
                // 获取锁师失败，休眠后重试
                Thread.sleep(tryLockSleepTime);
            }
            // 用尽所有尝试次数仍未获取到锁，抛出异常
            throw new CacheException(String.format("Failed to acquire user cache lock, tried %d times", maxTryLockTimes));
        } catch (CacheException e) {
            // 直接向上抛出 CacheException，保留异常信息
            throw e;
        } catch (Exception e) {
            // 捕获其他异常（如 InterruptedException），统一包装为 CacheException
            throw new CacheException(e);
        } finally {
            // 确保分布式锁被正确释放，避免死锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 根据 ID 删除缓存数据
     * <p>
     * 用于数据更新或删除后清除缓存，保证缓存与数据源的最终一致性。
     * </p>
     * <p>
     * 使用场景：
     * <ul>
     *   <li>数据修改后：删除缓存，下次查询时重新加载最新数据</li>
     *   <li>数据删除后：清除缓存，避免返回已删除的数据</li>
     * </ul>
     * </p>
     * <p>
     * 注意：该方法采用 Cache-Aside 模式，只删除缓存不更新缓存，
     * 更新操作由下次查询触发（懒加载），避免了更新失败的风险。
     * </p>
     *
     * @param keyPrefix 缓存键前缀
     * @param id        要删除的数据 ID
     */
    public void removeCache(String keyPrefix, K id) {
        // 拼接完整的 Redis 键并删除
        this.stringRedisTemplate.delete(RedisCacheConstants.CACHE_KEY_PREFIX + keyPrefix + ":" + id);
    }
}
