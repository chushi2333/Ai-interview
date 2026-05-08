package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 收藏列表中的单条题目摘要，给前端展示“我收藏了哪些题”
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionFavoriteVo {
    // 收藏记录 ID
    private Long id;

    // 题目 ID
    private Long questionId;

    // 题目标题
    private String questionTitle;

    // 题目难度
    private Integer questionDifficulty;

    // 收藏时间
    private LocalDateTime createTime;
}
