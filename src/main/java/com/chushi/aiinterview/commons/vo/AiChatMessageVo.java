package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiChatMessageVo {
    private Long id;

    private Long sessionId;

    private Long questionId;

    private String role;

    private String content;

    private String modelName;

    private String status;

    private String errorMessage;

    private Long latencyMs;

    private LocalDateTime createTime;
}
