package com.chushi.aiinterview.commons.utils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import com.chushi.aiinterview.annotations.validations.PhoneNumber;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
    private String region;

    @Override
    public void initialize(PhoneNumber constraintAnnotation) { region = constraintAnnotation.region(); }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            return phoneNumberUtil.isValidNumber(phoneNumberUtil.parse(value, region));
        } catch (NumberParseException exception) {
            return false;
        }
    }
}
