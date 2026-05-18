package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.utils.RedisJwtUtil;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.UserMapper;
import com.chushi.aiinterview.commons.utils.cache.PreconfiguredRedisCacheTemplate;
import com.chushi.aiinterview.services.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private PreconfiguredRedisCacheTemplate<Long, User> userRedisCacheTemplate;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Resource
    private RedisJwtUtil redisJwtUtil;

    @Override
    public Optional<User> getUserById(Long id)  {
        return userRedisCacheTemplate.queryById(id);
    }

    @Override
    public User updateAvatar(Long userId, String avatar) {
        var user = userMapper.findById(userId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "用户不存在")
        );
        user.setAvatar(avatar);

        var affectedRows = userMapper.updateAvatar(user);
        if (affectedRows != 1) {
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "头像更新失败，请稍后再试");
        }

        userRedisCacheTemplate.removeCache(userId);
        return user;
    }

    @Override
    public User updateNickname(Long userId, String nickname) {
        var user = userMapper.findById(userId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "用户不存在")
        );
        user.setNickname(nickname.trim());

        var affectedRows = userMapper.updateNickname(user);
        if (affectedRows != 1) {
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "昵称更新失败，请稍后再试");
        }

        userRedisCacheTemplate.removeCache(userId);
        return user;
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        var user = userMapper.findById(userId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "用户不存在")
        );

        if (StringUtils.hasText(user.getPassword())) {
            if (!StringUtils.hasText(oldPassword)) {
                throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "请输入旧密码");
            }

            if (!bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) {
                throw new BusinessException(HttpServletResponse.SC_UNAUTHORIZED, "旧密码不正确");
            }
        }

        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        var affectedRows = userMapper.updatePassword(user);
        if (affectedRows != 1) {
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "密码更新失败，请稍后再试");
        }

        userRedisCacheTemplate.removeCache(userId);
        redisJwtUtil.deleteUserToken(userId);
    }
}
