package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 自测题实体，挂在学习题目下面做客观检测
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSelfTest {
    // 自测题主键
    private Long id;

    // 关联的学习题目
    private Long questionId;

    // 自测题题干
    private String content;

    // 提交后返回给用户的题目解析
    private String explanation;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;

    // 软删除标记
    private Integer isDelete;
}
