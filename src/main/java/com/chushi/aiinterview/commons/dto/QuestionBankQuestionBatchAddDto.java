package com.chushi.aiinterview.commons.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionBankQuestionBatchAddDto {
    @NotNull(message = "questionBankId must not be null")
    private Long questionBankId;

    @NotEmpty(message = "questionIds must not be empty")
    private List<Long> questionIds;
}
