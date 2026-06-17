package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// AI 助教调用记录实体，表示用户对某道题发起的一次 AI 辅助请求
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAssistRecord {
    private Long id;

    private Long userId;

    private Long questionId;

    private String assistType;

    private String userInput;

    private String content;

    private String modelName;

    private String status;

    private String errorMessage;

    private Long latencyMs;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
