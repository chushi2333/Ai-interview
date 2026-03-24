package com.chushi.aiinterview.services;

public interface ShortMessageService {
    void sendCaptchaCode(String phone, String ip);

    boolean validateCaptchaCode(String phone, String captchaCode);
}
