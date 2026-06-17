package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuestionSelfTestSubmitDto {
    // 用户选择的单个选项
    @NotBlank(message = "Selected answer is required")
    private String selectedAnswer;

    // 作答耗时，单位秒
    @PositiveOrZero(message = "Duration seconds must be >= 0")
    private Integer durationSeconds;
}
