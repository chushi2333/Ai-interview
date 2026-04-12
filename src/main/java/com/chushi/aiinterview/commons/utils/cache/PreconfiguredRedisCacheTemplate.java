package com.chushi.aiinterview.commons.utils.cache;

import jakarta.annotation.Resource;

import java.util.Optional;
import java.util.function.Function;

/**
 * 预配置的 Redis 缓存模板类
 * <p>
 * 该类继承自 {@link BaseRedisCacheTemplate}，提供了预先配置默认参数的缓存操作模板。
 * 通过预设键前缀、逻辑过期时间和默认查询方法，简化了缓存操作的调用，避免重复传递相同参数。
 * </p>
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>支持缓存的存储、获取、刷新和删除操作</li>
 *   <li>内置防缓存穿透、击穿、雪崩机制</li>
 *   <li>支持逻辑过期和物理过期双重保护</li>
 *   <li>提供异步缓存刷新能力</li>
 * </ul>
 * </p>
 *
 * @param <K> 缓存键的类型
 * @param <V> 缓存值的类型
 * @author ChuShi
 * @see BaseRedisCacheTemplate
 * @see RedisCacheData
 * @see RedisCacheProperties
 */
public class PreconfiguredRedisCacheTemplate<K, V> extends BaseRedisCacheTemplate<K, V> {
    /**
     * 默认逻辑过期时间（秒）
     * <p>
     * 用于标记缓存数据的逻辑失效时间，当缓存数据逻辑过期后会触发异步刷新，
     * 但在物理过期前仍可返回旧数据，避免缓存击穿。
     * </p>
     */
    private final Long defaultLogicTTL;

    /**
     * 默认数据查询方法
     * <p>
     * 当缓存未命中或需要刷新时，使用该函数从数据源（如数据库）获取数据。
     * 该函数接收缓存键作为参数，返回包装在 Optional 中的值，支持空值处理。
     * </p>
     */
    private final Function<K, Optional<V>> defaultQueryMethod;

    /**
     * 默认缓存键前缀
     * <p>
     * 用于区分不同业务的缓存数据，避免键冲突。
     * 实际存储的 Redis 键格式为：{defaultKeyPrefix}:{id}
     * </p>
     */
    private final String defaultKeyPrefix;

    /**
     * Redis 缓存配置属性
     * <p>
     * 包含物理过期延迟、随机过期延迟、锁等待时间、锁租期等配置参数，
     * 用于控制缓存的过期策略和分布式锁行为。
     * </p>
     */
    @Resource
    private RedisCacheProperties redisCacheProperties;

    /**
     * 构造函数
     * <p>
     * 初始化预配置的 Redis 缓存模板，设置默认的键前缀、逻辑过期时间和查询方法。
     * </p>
     *
     * @param defaultKeyPrefix   默认缓存键前缀，用于区分不同业务的缓存
     * @param defaultLogicTTL    默认逻辑过期时间（秒），用于标记缓存的逻辑失效时间
     * @param defaultQueryMethod 默认数据查询方法，用于从数据源获取数据
     * @param valueType          缓存值的类型 Class 对象，用于反序列化
     */
    public PreconfiguredRedisCacheTemplate(String defaultKeyPrefix, Long defaultLogicTTL, Function<K, Optional<V>> defaultQueryMethod, Class<V> valueType) {
        super(valueType);
        this.defaultLogicTTL = defaultLogicTTL;
        this.defaultQueryMethod = defaultQueryMethod;
        this.defaultKeyPrefix = defaultKeyPrefix;
    }

    /**
     * 设置缓存
     * <p>
     * 将数据存储到 Redis 中，包含逻辑过期时间和物理过期时间。
     * 物理过期时间会在逻辑过期时间的基础上增加随机延迟，防止缓存雪崩。
     * </p>
     *
     * @param id    缓存键ID，与默认键前缀组合形成完整的 Redis 键
     * @param value 要缓存的值，将被序列化后存储到 Redis
     */
    public void setCache(K id, V value) {
        super.setCache(defaultKeyPrefix, id, value, defaultLogicTTL, redisCacheProperties.getPhysicalExpireDelay(), redisCacheProperties.getMaxRandomExpireDelay());
    }

    /**
     * 获取缓存
     * <p>
     * 从 Redis 中获取指定 ID 的缓存数据，返回包含数据和逻辑过期时间的 {@link RedisCacheData} 对象。
     * 如果缓存不存在或已物理过期，返回 {@link Optional#empty()}。
     * </p>
     *
     * @param id 缓存键ID
     * @return 缓存数据的 Optional 包装，包含数据本身和逻辑过期时间；如果不存在则返回空 Optional
     */
    public Optional<RedisCacheData<V>> getCache(K id) {
        return super.getCache(defaultKeyPrefix, id);
    }

    /**
     * 刷新缓存
     * <p>
     * 使用默认查询方法从数据源获取最新数据并更新缓存。
     * 如果数据源返回空值，会缓存空值（使用 {@link RedisCacheData} 的特殊标记）以防止缓存穿透。
     * </p>
     *
     * @param id 缓存键ID
     * @return 更新后的缓存数据的 Optional 包装；如果数据源返回空值则返回空 Optional
     */
    public Optional<V> refreshCache(K id) {
        return super.refreshCache(defaultKeyPrefix, id, defaultQueryMethod, defaultLogicTTL, redisCacheProperties.getPhysicalExpireDelay(), redisCacheProperties.getMaxRandomExpireDelay());
    }

    /**
     * 根据ID查询数据
     * <p>
     * 使用多级缓存策略查询数据，流程如下：
     * <ol>
     *   <li>首先尝试从 Redis 缓存获取数据</li>
     *   <li>如果缓存命中且未逻辑过期，直接返回缓存数据</li>
     *   <li>如果缓存逻辑过期，使用分布式锁异步刷新缓存，同时返回旧数据（防止缓存击穿）</li>
     *   <li>如果缓存未命中，使用分布式锁从数据源加载数据并缓存（防止缓存击穿）</li>
     *   <li>如果数据源返回空值，缓存空值防止缓存穿透</li>
     * </ol>
     * </p>
     *
     * @param id 要查询的数据ID
     * @return 查询到的数据的 Optional 包装；如果数据不存在则返回空 Optional
     */
    public Optional<V> queryById(K id) {
        return super.queryById(defaultKeyPrefix, id, defaultQueryMethod, redisCacheProperties.getMaxTryLockTimes(), redisCacheProperties.getTryLockSleepTime(), defaultLogicTTL, redisCacheProperties.getPhysicalExpireDelay(), redisCacheProperties.getMaxRandomExpireDelay(), redisCacheProperties.getLockWaitTime(), redisCacheProperties.getLockLeaseTime());
    }

    /**
     * 根据ID删除缓存数据
     * <p>
     * 从 Redis 中删除指定 ID 的缓存数据。
     * 通常在数据更新或删除后调用，保证缓存与数据源的一致性。
     * </p>
     *
     * @param id 要删除的缓存键ID
     */
    public void removeCache(K id) {
        super.removeCache(defaultKeyPrefix, id);
    }
}
