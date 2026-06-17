package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiRagQuestionIndexVo {
    private Long questionId;

    private Integer deletedChunkCount;

    private Integer indexedChunkCount;

    private Integer chunkContentLength;

    private String embeddingModelName;

    private Integer embeddingDimension;

    private Boolean dimensionMatched;
}
