package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiRagQuestionBatchIndexVo {
    private Integer requestedCount;

    private Integer successCount;

    private Integer failedCount;

    private List<AiRagQuestionBatchIndexItemVo> items;
}
