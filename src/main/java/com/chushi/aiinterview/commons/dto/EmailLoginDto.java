package com.chushi.aiinterview.commons.dto;

import com.chushi.aiinterview.annotations.validations.Password;
import com.chushi.aiinterview.annotations.validations.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailLoginDto {
    @Schema(description = "邮箱")
    @PhoneNumber
    private String email;

    @Schema(description = "密码")
    @Password
    private String password;
}
