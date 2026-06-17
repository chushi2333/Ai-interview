package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiRagSearchDebugDto {
    @NotBlank(message = "query must not be empty")
    @Size(max = 1000, message = "query length must be less than 1000")
    private String query;

    @Min(value = 1, message = "topK must be >= 1")
    @Max(value = 10, message = "topK must be <= 10")
    private Integer topK = 5;
}
