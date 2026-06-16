package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// RAG 知识片段实体。embedding 用字符串承载，写入 PostgreSQL 时由 SQL 转成 vector 类型。
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiRagChunk {
    private Long id;

    private Long questionId;

    private Integer chunkIndex;

    private String sourceType;

    private String title;

    private String content;

    private String embedding;

    private String metadata;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
