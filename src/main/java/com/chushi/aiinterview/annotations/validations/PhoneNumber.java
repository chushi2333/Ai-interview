package com.chushi.aiinterview.annotations.validations;

import com.chushi.aiinterview.commons.utils.PhoneNumberValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface PhoneNumber {
    String message() default "Invalid phone number format";

    String region() default "CN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
