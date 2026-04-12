package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.utils.cache.PreconfiguredRedisCacheTemplate;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Resource
    private PreconfiguredRedisCacheTemplate<Long, User> userRedisCacheTemplate;

    @Override
    public Optional<User> getUserById(Long id)  {
        return userRedisCacheTemplate.queryById(id);
    }
}
