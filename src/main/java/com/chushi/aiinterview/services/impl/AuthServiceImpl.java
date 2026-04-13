package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.utils.JwtUtil;
import com.chushi.aiinterview.commons.utils.RedisJwtUtil;
import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.services.ShortMessageService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.mappers.UserMapper;
import com.chushi.aiinterview.services.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private IdGenerator<Long> userIdGenerator;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RedisJwtUtil redisJwtUtil;

    @Resource
    private ShortMessageService shortMessageService;


    @Override
    public User registerViaPhone(String phone, String password) {
        var existingUser = userMapper.findByPhone(phone);
        existingUser.ifPresent(user -> {
            throw new BusinessException(HttpServletResponse.SC_CONFLICT, "Phone already exists");
        });

        var user = new User();
        user.setId(userIdGenerator.nextId());
        user.setPhone(phone);
        user.setJoinTime(TimeUtils.currentLocalDateTime());

        var encodedPassword = bCryptPasswordEncoder.encode(password);
        user.setPassword(encodedPassword);

        var affectedRows = userMapper.insert(user);
        if (affectedRows > 0) {
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Register failed");
        }

        return user;
    }

    @Override
    public String loginViaPhone(String phone, String password) {
        var user = userMapper.findByPhone(phone).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Phone not found")
        );

        return loginWithPassword(user, password);
    }

    @Override
    public String loginViaEmail(String email, String password) {
        var user = userMapper.findByEmail(email).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Email not found")
        );

        return loginWithPassword(user, password);
    }

    private String loginWithPassword(User user, String password) {
        if (user.getPassword() == null) {
            throw new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Password not been set");
        }
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(HttpServletResponse.SC_UNAUTHORIZED, "Wrong password");
        }
        var token =  jwtUtil.generateToken(user);
        redisJwtUtil.setUserToken(user.getId(), token, jwtUtil.getExpiration());

        return token;
    }

    @Override
    public String loginViaSMS(String phone, String captchaCode) {
        log.info("loginViaSMS: phone='{}', captchaCode='{}'", phone, captchaCode);
        if (!shortMessageService.validateCaptchaCode(phone, captchaCode)) {
            throw new BusinessException(HttpServletResponse.SC_UNAUTHORIZED, "Invalid captcha code");
        }

        var user = userMapper.findByPhone(phone).orElseGet(() -> {
            // 用户不存在注册用户
            var newUser = new User();
            newUser.setId(userIdGenerator.nextId());
            newUser.setPhone(phone);
            newUser.setJoinTime(TimeUtils.currentLocalDateTime());

            var affectedRows = userMapper.insert(newUser);
            if (affectedRows != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Register failed");
            }

            return newUser;
        });

        var token = jwtUtil.generateToken(user);
        redisJwtUtil.setUserToken(user.getId(), token, jwtUtil.getExpiration());

        return token;
    }
}
