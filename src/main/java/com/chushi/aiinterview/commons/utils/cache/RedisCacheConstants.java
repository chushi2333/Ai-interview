package com.chushi.aiinterview.commons.utils.cache;

/**
 * Redis 缓存工具相关常量类
 * <p>
 * 定义了 Redis 缓存系统中使用的统一键前缀，确保不同模块的缓存数据和锁资源有清晰的命名空间。
 * </p>
 * <p>
 * 键命名规范：
 * <ul>
 *   <li>缓存键：{@code cache:{业务前缀}:{ID}}</li>
 *   <li>锁键：{@code lock:cache:{业务前缀}{ID}}</li>
 * </ul>
 * </p>
 *
 * @author ChuShi
 * @see BaseRedisCacheTemplate
 * @see PreconfiguredRedisCacheTemplate
 */
public class RedisCacheConstants {
    /**
     * 缓存键统一前缀
     * <p>
     * 所有缓存数据的 Redis 键都会以 {@code "cache:"} 开头，用于：
     * <ul>
     *   <li>区分缓存数据与其他业务数据（如消息队列、会话数据等）</li>
     *   <li>便于批量操作（如清理所有缓存）</li>
     *   <li>方便监控和统计缓存使用情况</li>
     * </ul>
     * </p>
     * <p>
     * 完整键格式示例：{@code cache:user:12345}
     * </p>
     */
    static final String CACHE_KEY_PREFIX = "cache:";

    /**
     * 缓存分布式锁键统一前缀
     * <p>
     * 所有缓存相关的分布式锁键都会以 {@code "lock:cache:"} 开头，用于：
     * <ul>
     *   <li>防止缓存击穿：确保同一时刻只有一个线程从数据库加载数据</li>
     *   <li>保证缓存更新的原子性：避免并发更新导致的数据不一致</li>
     *   <li>与业务锁隔离：缓存锁与业务逻辑锁使用不同的命名空间</li>
     * </ul>
     * </p>
     * <p>
     * 完整锁键格式示例：{@code lock:cache:user12345}
     * </p>
     */
    static final String CACHE_LOCK_KEY_PREFIX = "lock:cache:";
}
