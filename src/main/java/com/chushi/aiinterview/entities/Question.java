package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 题目实体类，表示系统中的题目
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Question {
    // 题目的唯一标识符
    private Long id;

    // 题目标题
    private String title;

    // 题目内容
    private String content;

    // 题目标签列表，使用 JSON 数组字符串存储
    private String tags;

    // 题目推荐答案
    private String answer;

    // 创建用户 id
    private Long userId;

    // 编辑时间
    private LocalDateTime editTime;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;

    // 是否删除
    private Integer isDelete;
}
