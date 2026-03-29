package com.chushi.aiinterview.commons.dto;

import com.chushi.aiinterview.annotations.validations.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SMSLoginDto {
    @Schema(description = "手机号")
    @PhoneNumber
    private String phone;

    @Schema(description = "验证码")
    @Pattern(regexp = "^[0-9]{6}$", message = "Invalid captchaCode")
    private String captchaCode;
}
