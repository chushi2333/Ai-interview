package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.NoAuth;
import com.chushi.aiinterview.commons.dto.PhoneRegisterDto;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.commons.vo.UserRegisterVo;
import com.chushi.aiinterview.services.AuthService;
import com.chushi.aiinterview.services.ShortMessageService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class Authcontroller extends BaseController {
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
}
