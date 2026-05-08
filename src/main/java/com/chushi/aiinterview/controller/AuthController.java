package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.NoAuth;
import com.chushi.aiinterview.annotations.validations.PhoneNumber;
import com.chushi.aiinterview.commons.dto.EmailLoginDto;
import com.chushi.aiinterview.commons.dto.PhoneLoginDto;
import com.chushi.aiinterview.commons.dto.PhoneRegisterDto;
import com.chushi.aiinterview.commons.dto.SMSLoginDto;
import com.chushi.aiinterview.commons.utils.IPUtils;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.commons.vo.UserLoginVo;
import com.chushi.aiinterview.commons.vo.UserRegisterVo;
import com.chushi.aiinterview.services.AuthService;
import com.chushi.aiinterview.services.ShortMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController extends BaseController {
    @Resource
    private ShortMessageService shortMessageService;

    @Resource
    private AuthService authService;

    @PostMapping("/register-via-phone")
    @NoAuth
    @Operation(summary = "手机注册")
    public Response<UserRegisterVo> registerViaPhone(
            @Valid @RequestBody PhoneRegisterDto phoneRegisterDto
    ) {
        var user = authService.registerViaPhone(phoneRegisterDto.getPhone(), phoneRegisterDto.getPassword());
        return wrap(new UserRegisterVo(user.getId()));
    }

    @PostMapping("/login-via-email")
    @NoAuth
    @Operation(summary = "邮箱密码登陆")
    public Response<UserLoginVo> loginViaEmail(
            @Valid @RequestBody EmailLoginDto emailLoginDto
    ) {
        var token = authService.loginViaEmail(emailLoginDto.getEmail(), emailLoginDto.getPassword());
        return wrap(new UserLoginVo(token));
    }

    @PostMapping("/login-via-phone")
    @NoAuth
    @Operation(summary = "手机密码登陆")
    public Response<UserLoginVo> loginViaPhone(
            @Valid @RequestBody PhoneLoginDto phoneLoginDto
    ) {
        var token = authService.loginViaPhone(phoneLoginDto.getPhone(), phoneLoginDto.getPassword());
        return wrap(new UserLoginVo(token));
    }

    @PostMapping("/login-via-sms")
    @NoAuth
    @Operation(summary = "短信登陆")
    public Response<UserLoginVo> loginViaSMS(
            @Valid @RequestBody SMSLoginDto smsLoginDto
    ) {
        log.info("controller dto = {}", smsLoginDto);
        var token = authService.loginViaSMS(smsLoginDto.getPhone(), smsLoginDto.getCaptchaCode());
        return wrap(new UserLoginVo(token));
    }

    @PostMapping("/captcha/sms")
    @NoAuth
    @Operation(summary = "获取短信验证码")
    public Response<Void> getSMSCaptcha(
            @Parameter(description = "手机号") @Valid @RequestParam @PhoneNumber String phone,
            HttpServletRequest request
    ) {
        shortMessageService.sendCaptchaCode(phone, IPUtils.getIpAddress(request));
        return wrap();
    }

}
