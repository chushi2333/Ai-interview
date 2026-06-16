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
public class AiChatMessageSendVo {
    private AiChatMessageVo userMessage;

    private AiChatMessageVo assistantMessage;

    private Boolean ragEnabled;

    private Integer ragChunkCount;

    private String ragDecisionReason;

    private String ragDecisionStrategy;

    private List<AiRagChunkSearchVo> ragChunks;

    public AiChatMessageSendVo(AiChatMessageVo userMessage, AiChatMessageVo assistantMessage) {
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
    }
}
