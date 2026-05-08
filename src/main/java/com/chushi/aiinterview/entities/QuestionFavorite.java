package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 题目收藏记录，表示用户主动收藏了一道题目
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionFavorite {
    // 收藏记录主键
    private Long id;

    // 收藏用户 ID
    private Long userId;

    // 被收藏的题目 ID
    private Long questionId;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
