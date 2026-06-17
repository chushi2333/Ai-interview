package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiEmbeddingDebugDto {
    @NotBlank(message = "text must not be empty")
    @Size(max = 2000, message = "text length must be less than 2000")
    private String text;
}
