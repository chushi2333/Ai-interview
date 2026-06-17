package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 题库实体类，表示系统中的题库
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionBank {
    // 题库的唯一标识符
    private Long id;

    // 题库标题
    private String title;

    // 题库描述
    private String description;

    // 题库封面图片
    private String picture;

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
