package com.chushi.aiinterview.commons.dto;

import com.chushi.aiinterview.annotations.validations.Password;
import com.chushi.aiinterview.annotations.validations.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PhoneRegisterDto {
    @Schema(description = "手机号")
    @PhoneNumber
    private String phone;

    @Schema(description = "密码")
    @Password
    private String password;
}
