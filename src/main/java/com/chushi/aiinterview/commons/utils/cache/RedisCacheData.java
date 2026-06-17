package com.chushi.aiinterview.commons.utils.cache;

import lombok.Data;

/**
 * Redis 缓存数据包装类
 * <p>
 * 封装实际缓存的业务数据和逻辑过期时间，实现逻辑过期机制。
 * 该类是缓存系统的核心数据结构，所有存储到 Redis 的数据都会被包装成此类型。
 * </p>
 * <p>
 * 逻辑过期机制说明：
 * <ul>
 *   <li><b>物理过期</b>：Redis 键的 TTL，到期后 Redis 自动删除，用于清理长期无人访问的数据</li>
 *   <li><b>逻辑过期</b>：存储在数据中的过期时间戳，到期后数据仍存在，但会触发异步刷新</li>
 *   <li><b>优势</b>：逻辑过期时返回旧数据而非阻塞等待，保证服务高可用性</li>
 * </ul>
 * </p>
 * <p>
 * 存储示例（JSON 格式）：
 * <pre>
 * {
 *   "expireTime": 1704067200,   // Unix 时间戳（秒级）
 *   "value": {                  // 实际业务对象
 *     "id": 123,
 *     "name": "张三",
 *     "email": "zhangsan@example.com"
 *   }
 * }
 * </pre>
 * </p>
 * <p>
 * 防缓存穿透的特殊处理：
 * 当数据库中不存在某条记录时，{@code value} 会被设置为 {@code null}，
 * 这样可以缓存"数据不存在"的状态，避免恶意请求不存在的数据导致数据库压力。
 * </p>
 *
 * @param <T> 实际缓存的业务数据类型（如 User、Product 等）
 * @author ChuShi
 * @see BaseRedisCacheTemplate
 */
@Data
public class RedisCacheData<T> {
    /**
     * 逻辑过期时间
     * <p>
     * Unix 时间戳，单位为秒（不是毫秒）。表示缓存数据在业务逻辑上的有效期。
     * </p>
     * <p>
     * 使用说明：
     * <ul>
     *   <li>当前时间戳大于此值时，表示数据逻辑上已过期，需要刷新</li>
     *   <li>逻辑过期后不会立即删除数据，而是异步更新，旧数据仍可返回</li>
     *   <li>通过 {@link java.time.Instant#ofEpochSecond(long)} 可转换为 Instant 对象进行比较</li>
     * </ul>
     * </p>
     * <p>
     * 计算方式：{@code Instant.now().plusSeconds(logicalTTL).getEpochSecond()}
     * </p>
     */
    private Long expireTime;

    /**
     * 实际缓存的业务数据
     * <p>
     * 存储真正的业务对象，可以是任意类型（User、Product、Order 等）。
     * </p>
     * <p>
     * 特殊值处理：
     * <ul>
     *   <li><b>正常情况</b>：存储完整的业务对象</li>
     *   <li><b>数据不存在</b>：{@code value = null}，用于防止缓存穿透</li>
     *   <li><b>序列化</b>：通过 Jackson 序列化为 JSON 字符串存储到 Redis</li>
     *   <li><b>反序列化</b>：从 Redis 读取 JSON 字符串后反序列化为对象</li>
     * </ul>
     * </p>
     * <p>
     * 注意：虽然 {@code value} 可以为 {@code null}（防穿透），但 {@code expireTime} 必须有值。
     * </p>
     */
    private T value;
}
