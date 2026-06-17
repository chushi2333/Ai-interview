package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiQuestionAssistRequestDto {
    @NotBlank(message = "type must not be empty")
    private String type;

    @Size(max = 2000, message = "user input length must be less than 2000")
    private String userInput;
}
