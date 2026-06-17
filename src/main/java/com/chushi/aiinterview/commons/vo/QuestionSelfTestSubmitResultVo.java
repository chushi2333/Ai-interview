package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSelfTestSubmitResultVo {
    // 自测记录ID
    private Long recordId;

    // 自测题ID
    private Long selfTestId;

    // 关联的学习题目ID
    private Long questionId;

    // 判题结果：0错 1对
    private Integer isCorrect;

    // 用户本次选择的答案
    private String selectedAnswer;

    // 系统正确答案
    private String correctAnswer;

    // 题目解析
    private String explanation;
}
