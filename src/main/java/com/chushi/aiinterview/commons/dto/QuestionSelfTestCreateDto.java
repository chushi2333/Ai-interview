package com.chushi.aiinterview.commons.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class QuestionSelfTestCreateDto {
    // 自测题题干
    @NotBlank(message = "Content is required")
    @Size(max = 1024, message = "Content length must be <= 1024")
    private String content;

    // 做完题后展示给用户的解析
    @Size(max = 2000, message = "Explanation length must be <= 2000")
    private String explanation;

    // 单选题选项列表，第一版固定按单选题处理
    @Valid
    @NotEmpty(message = "Options are required")
    @Size(min = 2, max = 6, message = "Options size must be between 2 and 6")
    private List<QuestionSelfTestOptionCreateDto> options;
}
