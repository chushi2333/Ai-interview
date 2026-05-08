package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.UserMapper;
import com.chushi.aiinterview.commons.utils.cache.PreconfiguredRedisCacheTemplate;
import com.chushi.aiinterview.services.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private PreconfiguredRedisCacheTemplate<Long, User> userRedisCacheTemplate;

    @Override
    public Optional<User> getUserById(Long id)  {
        return userRedisCacheTemplate.queryById(id);
    }

    @Override
    public User updateAvatar(Long userId, String avatar) {
        var user = userMapper.findById(userId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "User not found")
        );
        user.setAvatar(avatar);

        var affectedRows = userMapper.updateAvatar(user);
        if (affectedRows != 1) {
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Update avatar failed");
        }

        userRedisCacheTemplate.removeCache(userId);
        return user;
    }
}
