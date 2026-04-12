package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.dto.ShortMessageCodeDto;
import com.chushi.aiinterview.configurations.ShortMessageRabbitConfiguration;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.services.ShortMessageService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ShortMessageServiceImpl implements ShortMessageService {
    private static final String SMS_CODE_KEY_PREFIX = "sms:code";
    private static final String SMS_CODE_STORE_KEY = ":store:";

    private static final long PHONE_CAPTCHA_CODE_TIMEOUT = 5;
    private final DefaultRedisScript<String> rateLimitScript;
    @Resource
    private SecureRandom secureRandom;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    ShortMessageServiceImpl(ResourceLoader resourceLoader) {
        try (var resourceStream = resourceLoader.getResource("classpath:redis-scripts/sms_code_rate_limit.lua").getInputStream()) {
            var script = StreamUtils.copyToString(resourceStream, StandardCharsets.UTF_8);
            rateLimitScript = new DefaultRedisScript<>();
            rateLimitScript.setScriptText(script);
            rateLimitScript.setResultType(String.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendCaptchaCode(String phone, String ip) {
        // 准备参数
        var now = String.valueOf(System.currentTimeMillis());
        var keys = List.of(SMS_CODE_KEY_PREFIX);

        // 获取限流校验结果
        var result = stringRedisTemplate.execute(rateLimitScript, keys, phone, ip, now);

        // 如果不通过限流校验
        if (!result.equals("OK")) {
            // 疑似不正常请求验证码行为 日志记录
            if (!result.equals("TOO_FREQUENT_PHONE_1M")) {
                log.warn("SMSRateLimiterRefused: phone = {}, ip = {}, reason = {}", phone, ip, result);
            }
            throw new BusinessException(HttpServletResponse.SC_FORBIDDEN, "SMS sent too frequently");
        }

        // 生成并设置验证码
        var captchaCode = String.format("%06d", secureRandom.nextInt(1000000));
        stringRedisTemplate.opsForValue().set(SMS_CODE_KEY_PREFIX + SMS_CODE_STORE_KEY + phone, captchaCode, PHONE_CAPTCHA_CODE_TIMEOUT, TimeUnit.MINUTES);

        // 验证码发送
        var message = new ShortMessageCodeDto();
        message.setPhoneNumber(phone);
        message.setCode(captchaCode);

        rabbitTemplate.convertAndSend(
                ShortMessageRabbitConfiguration.SHORT_MESSAGE_CODE_EXCHANGE,
                ShortMessageRabbitConfiguration.SHORT_MESSAGE_CODE_ROUTING_KEY,
                message
        );

    }

    @Override
    public boolean validateCaptchaCode(String phone, String code) {
        var storedCaptchaCode = stringRedisTemplate.opsForValue().get(SMS_CODE_KEY_PREFIX + SMS_CODE_STORE_KEY + phone);
        if (storedCaptchaCode != null && storedCaptchaCode.equals(code)) {
            stringRedisTemplate.delete(SMS_CODE_KEY_PREFIX + SMS_CODE_STORE_KEY + phone);
            return true;
        }
        return false;
    }
}
