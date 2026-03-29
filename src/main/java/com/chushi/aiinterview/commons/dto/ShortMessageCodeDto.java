package com.chushi.aiinterview.commons.dto;

import com.chushi.aiinterview.annotations.validations.PhoneNumber;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ShortMessageCodeDto {
    @PhoneNumber
    private String phoneNumber;

    @Pattern(regexp = "^[0-9]{6}$")
    private String code;
}
