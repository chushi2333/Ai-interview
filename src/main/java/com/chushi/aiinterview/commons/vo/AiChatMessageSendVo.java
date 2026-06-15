package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiChatMessageSendVo {
    private AiChatMessageVo userMessage;

    private AiChatMessageVo assistantMessage;
}
