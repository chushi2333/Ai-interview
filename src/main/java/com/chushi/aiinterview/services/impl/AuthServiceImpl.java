package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.exceptions.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.mappers.UserMapper;
import com.chushi.aiinterview.services.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserMapper userMapper;

    @Override
    public User registerViaPhone(String phone, String password) {
        return null;
    }

    @Override
    public String loginViaPhone(String phone, String password) {
        return "";
    }

    @Override
    public String loginViaEmail(String email, String password) {
        return "";
    }

    @Override
    public String loginViaSMS(String phone, String captchaCode) {
        return "";
    }
}
