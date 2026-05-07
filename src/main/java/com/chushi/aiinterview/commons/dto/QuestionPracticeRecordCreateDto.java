package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class QuestionPracticeRecordCreateDto {
    @NotNull(message = "Question id is required")
    private Long questionId;

    @NotNull(message = "Is correct is required")
    @Min(value = 0, message = "Is correct must be 0 or 1")
    @Max(value = 1, message = "Is correct must be 0 or 1")
    private Integer isCorrect;

    @PositiveOrZero(message = "Duration seconds must be >= 0")
    private Integer durationSeconds;
}
