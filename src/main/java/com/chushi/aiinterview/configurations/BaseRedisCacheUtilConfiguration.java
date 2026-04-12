package com.chushi.aiinterview.configurations;

import com.chushi.aiinterview.commons.utils.cache.UtcLocalDateTimeModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

public abstract class BaseRedisCacheUtilConfiguration {
    @Bean
    public ThreadPoolTaskExecutor redisCacheRefreshTaskExecutor(
            @Value("${redis-cache.pool-refresh-task-executor.core-size:4}") int coreSize,
            @Value("${redis-cache.pool-refresh-task-executor.max-size:20}") int maxSize,
            @Value("${redis-cache.pool-refresh-task-executor.queue-capacity:100}") int queueCapacity,
            @Value("${redis-cache.pool-refresh-task-executor.executor-ttl:60}") int executorTTL
    ) {
        // 实例化线程池
        var refreshTaskExecutor = new ThreadPoolTaskExecutor();
        // 设置核心线程数 (IO 密集型一般 2 * CPU 数)
        refreshTaskExecutor.setCorePoolSize(coreSize);
        // 设置最大线程数 (一般 2 - 5 倍核心线程数)
        refreshTaskExecutor.setMaxPoolSize(maxSize);
        // 设置等待队列容量
        refreshTaskExecutor.setQueueCapacity(queueCapacity);
        // 设置非核心线程数存活时间
        refreshTaskExecutor.setKeepAliveSeconds(executorTTL);
        // 设置前缀
        refreshTaskExecutor.setThreadNamePrefix("cache-refresh-task-");
        // 设置拒绝策略：使用提交线程执行
        refreshTaskExecutor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        // 初始化线程池
        refreshTaskExecutor.initialize();

        return refreshTaskExecutor;
    }

    // 自定义序列化器，不暴露Redis使用的ObjectMapper，放置影响全局配置
    @Bean
    public RedisSerializer<Object> redisSerializer() {
        var objectMapper = new ObjectMapper();
        // 严格反序列化：遇到未知属性直接报错（防止缓存结构变更导致静默错误）
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        // 保证序列化完整性：包含 null 值（避免缓存和数据库状态不一致）
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // 统一时间格式为时间戳
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        // 禁用“美化”输出（节省 Redis 内存）
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        // 配置 LocalDateTime 的 UTC 序列化和反序列化
        objectMapper.registerModule(new UtcLocalDateTimeModule());
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
