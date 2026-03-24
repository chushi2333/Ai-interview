package com.chushi.aiinterview.services;

import com.chushi.aiinterview.entities.User;

public interface AuthService {
    User registerViaPhone(String phone, String password);

    String loginViaPhone(String phone, String password);

    String loginViaEmail(String email, String password);

    String loginViaSMS(String phone, String captchaCode);
}
