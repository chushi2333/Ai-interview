package com.chushi.aiinterview.configurations;

import com.chushi.aiinterview.commons.utils.cache.PreconfiguredRedisCacheTemplate;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.mappers.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisCacheConfiguration extends BaseRedisCacheUtilConfiguration {
    @Resource
    private UserMapper userMapper;

    @Bean
    public PreconfiguredRedisCacheTemplate<Long, User> userRedisTemplate() {
        return new PreconfiguredRedisCacheTemplate<>(
                "user",
                3600L,
                userMapper::findById,
                User.class
        );
    }
}
