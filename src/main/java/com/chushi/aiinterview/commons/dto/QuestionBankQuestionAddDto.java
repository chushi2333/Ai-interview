package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionBankQuestionAddDto {
    @NotNull(message = "questionBankId must not be null")
    private Long questionBankId;

    @NotNull(message = "questionId must not be null")
    private Long questionId;
}
