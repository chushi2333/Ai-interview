package com.chushi.aiinterview.annotations.validations;

import com.chushi.aiinterview.commons.utils.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/// 校验密码是否包含大写字母、小写字母、数字和特殊字符，且长度不少于8位
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(validatedBy = PasswordValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
    String message() default "The password strength is insufficient. It must include uppercase letters, lowercase letters, numbers, with a minimum length of 8 characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
