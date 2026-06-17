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
public class AiEmbeddingDebugVo {
    private String modelName;

    private Integer configuredDimension;

    private Integer actualDimension;

    private Boolean dimensionMatched;

    private List<Float> vectorPreview;

    private String pgVectorPreview;
}
