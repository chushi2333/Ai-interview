package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiRagQuestionBatchIndexItemVo {
    private Long questionId;

    private Boolean success;

    private String message;

    private AiRagQuestionIndexVo result;
}
