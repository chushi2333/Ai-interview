package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 自测题选项实体
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSelfTestOption {
    // 选项主键
    private Long id;

    // 关联的自测题
    private Long selfTestId;

    // 选项标识，例如 A/B/C/D
    private String optionKey;

    // 选项内容
    private String content;

    // 是否正确选项：0否 1是
    private Integer isCorrect;

    // 选项显示顺序
    private Integer sortOrder;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
