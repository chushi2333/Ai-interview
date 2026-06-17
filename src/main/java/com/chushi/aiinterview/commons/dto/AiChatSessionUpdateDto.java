package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatSessionUpdateDto {
    @NotBlank(message = "title must not be blank")
    @Size(max = 128, message = "title length must be less than 128")
    private String title;
}
