package com.chushi.aiinterview.annotations.validations;

import com.chushi.aiinterview.commons.utils.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/// 校验密码是否包含大写字母、小写字母和数字，且长度不少于 8 位
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(validatedBy = PasswordValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
    String message() default "密码强度不足，至少 8 位，且必须包含大写字母、小写字母和数字";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
