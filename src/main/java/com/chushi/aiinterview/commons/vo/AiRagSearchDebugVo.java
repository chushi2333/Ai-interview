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
public class AiRagSearchDebugVo {
    private String query;

    private Integer topK;

    private String embeddingModelName;

    private Integer embeddingDimension;

    private Integer matchedChunkCount;

    private List<AiRagChunkSearchVo> chunks;
}
