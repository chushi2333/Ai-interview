package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 自测题作答记录实体
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSelfTestRecord {
    // 自测记录主键
    private Long id;

    // 关联的自测题
    private Long selfTestId;

    // 作答用户
    private Long userId;

    // 用户选择的答案，当前按 JSON 数组字符串保存
    private String selectedAnswers;

    // 判题结果：0错 1对
    private Integer isCorrect;

    // 作答耗时，单位秒
    private Integer durationSeconds;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
