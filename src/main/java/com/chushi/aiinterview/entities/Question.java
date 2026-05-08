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

    // 题目描述或补充说明，可为空，避免和标题重复表达
    private String content;

    // 题目标签列表，使用 JSON 数组字符串存储
    private String tags;

    // 题解正文，后续允许承载 Markdown / 富文本内容
    private String answer;

    // 题目难度：1简单 2中等 3困难
    private Integer difficulty;

    // 是否仅会员可见
    private Integer isMemberOnly;

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
