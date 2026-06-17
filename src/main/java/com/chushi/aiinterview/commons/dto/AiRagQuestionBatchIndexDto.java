package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiRagQuestionBatchIndexDto {
    @Size(max = 50, message = "questionIds size must be less than 50")
    private List<Long> questionIds;

    @Min(value = 1, message = "limit must be greater than 0")
    @Max(value = 50, message = "limit must be less than 50")
    private Integer limit = 10;
}
