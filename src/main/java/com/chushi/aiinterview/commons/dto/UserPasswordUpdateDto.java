package com.chushi.aiinterview.commons.dto;

import com.chushi.aiinterview.annotations.validations.Password;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserPasswordUpdateDto {
    @Size(max = 128, message = "旧密码不能超过 128 个字符")
    private String oldPassword;

    @Password
    private String newPassword;
}
