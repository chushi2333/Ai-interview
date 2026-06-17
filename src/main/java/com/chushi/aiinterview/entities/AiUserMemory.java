package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 用户级长期学习记忆实体，跨 AI 对话会话保存用户的学习画像摘要
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiUserMemory {
    private Long id;

    private Long userId;

    private String memorySummary;

    private Integer sourceSessionCount;

    private Long lastSourceSessionId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
