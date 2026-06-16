package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiRagChunkSearchVo {
    private Long id;

    private Long questionId;

    private Integer chunkIndex;

    private String sourceType;

    private String title;

    private String content;

    private String metadata;

    private Double distance;
}
