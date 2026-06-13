package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// AI 对话会话实体，表示用户围绕某道题的一次多轮对话
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiChatSession {
    private Long id;

    private Long userId;

    private Long questionId;

    private String title;

    private String status;

    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
