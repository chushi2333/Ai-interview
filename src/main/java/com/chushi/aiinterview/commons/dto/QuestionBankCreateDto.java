package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionBankCreateDto {
    @NotBlank(message = "title must not be empty")
    @Size(max = 256, message = "title length must be less than 256")
    private String title;

    private String description;

    @Size(max = 2048, message = "picture length must be less than 2048")
    private String picture;
}
