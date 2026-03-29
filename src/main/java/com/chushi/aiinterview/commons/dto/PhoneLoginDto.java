package com.chushi.aiinterview.commons.dto;

import com.chushi.aiinterview.annotations.validations.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhoneLoginDto {
    @Schema(description = "手机号")
    @PhoneNumber
    private String phone;

    @Schema(description = "密码")
    @PhoneNumber
    private String password;
}
