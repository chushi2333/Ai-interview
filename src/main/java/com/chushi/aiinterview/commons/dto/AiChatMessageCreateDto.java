package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatMessageCreateDto {
    @NotBlank(message = "content must not be empty")
    @Size(max = 2000, message = "content length must be less than 2000")
    private String content;
}
