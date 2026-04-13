package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 题库题目关联实体类，表示题库与题目的多对多关系
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionBankQuestion {
    // 关联记录的唯一标识符
    private Long id;

    // 题库 id
    private Long questionBankId;

    // 题目 id
    private Long questionId;

    // 创建用户 id
    private Long userId;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
