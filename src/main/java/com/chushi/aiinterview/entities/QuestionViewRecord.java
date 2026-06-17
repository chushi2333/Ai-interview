package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 看题记录实体，表示用户某天看过某一道题
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionViewRecord {
    // 看题记录主键
    private Long id;

    // 当前查看题目的用户 id
    private Long userId;

    // 被查看的题目 id
    private Long questionId;

    // 查看日期，用于后续按天聚合统计
    private LocalDate viewDate;

    // 记录创建时间
    private LocalDateTime createTime;

    // 记录更新时间
    private LocalDateTime updateTime;
}
