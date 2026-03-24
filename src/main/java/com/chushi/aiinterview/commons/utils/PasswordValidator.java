package com.chushi.aiinterview.commons.utils;

import com.chushi.aiinterview.annotations.validations.Password;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<Password, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        // 长度检查
        if (value.length() < 8) {
            return false;
        }

        var hasLower = false;
        var hasUpper = false;
        var hasDigit = false;

        // 单次遍历检查字符类型
        for (int i = 0; i < value.length(); i++) {
            var ch = value.charAt(i);

            if (ch >= 'a' && ch <= 'z') {
                hasLower = true;
            } else if (ch >= 'A' && ch <= 'Z') {
                hasUpper = true;
            } else if (ch >= '0' && ch <= '9') {
                hasDigit = true;
            }

            // 提前终止：如果所有条件都已满足，可提前退出（可选优化）
            if (hasLower && hasUpper && hasDigit) {
                return true;
            }
        }

        return false;
    }
}
