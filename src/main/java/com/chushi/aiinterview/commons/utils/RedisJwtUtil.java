package com.chushi.aiinterview.commons.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class RedisJwtUtil {
    public static final String REDIS_USER_TOKEN_PREFIX = "token:user:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void setUserToken(Long userId, String token, long timeout) {
        var key = REDIS_USER_TOKEN_PREFIX + userId;
        stringRedisTemplate.opsForValue().set(key, token, timeout, TimeUnit.SECONDS);
    }

    public Optional<String> getUserToken(Long userId) {
        var key = REDIS_USER_TOKEN_PREFIX + userId;
        var token = stringRedisTemplate.opsForValue().get(key);
        return token != null ? Optional.of(token) : Optional.empty();
    }

    public void deleteUserToken(Long userId) {
        var key = REDIS_USER_TOKEN_PREFIX + userId;
        stringRedisTemplate.delete(key);
    }

    public boolean hasToken(Long userId) {
        var key = REDIS_USER_TOKEN_PREFIX + userId;
        return stringRedisTemplate.hasKey(key);
    }
}
