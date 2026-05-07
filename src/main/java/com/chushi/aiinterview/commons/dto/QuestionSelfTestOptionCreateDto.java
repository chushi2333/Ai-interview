package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuestionSelfTestOptionCreateDto {
    // 选项标识，例如 A/B/C/D
    @NotBlank(message = "Option key is required")
    @Size(max = 8, message = "Option key length must be <= 8")
    private String optionKey;

    // 选项内容
    @NotBlank(message = "Option content is required")
    @Size(max = 512, message = "Option content length must be <= 512")
    private String content;

    // 是否正确选项：0否 1是
    @NotNull(message = "Is correct is required")
    @Min(value = 0, message = "Is correct must be 0 or 1")
    @Max(value = 1, message = "Is correct must be 0 or 1")
    private Integer isCorrect;

    // 前端展示顺序
    @NotNull(message = "Sort order is required")
    @Min(value = 0, message = "Sort order must be >= 0")
    private Integer sortOrder;
}
