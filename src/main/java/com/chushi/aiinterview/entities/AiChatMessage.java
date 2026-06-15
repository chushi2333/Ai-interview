package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// AI 对话消息实体，保存用户消息和模型回复
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiChatMessage {
    private Long id;

    private Long sessionId;

    private Long userId;

    private Long questionId;

    private String role;

    private String content;

    private String modelName;

    private String status;

    private String errorMessage;

    private Long latencyMs;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
