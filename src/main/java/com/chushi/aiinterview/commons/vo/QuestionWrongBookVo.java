package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 错题本列表中的单条记录
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionWrongBookVo {
    // 最新一次错误记录 ID，同时作为分页游标使用
    private Long recordId;

    // 对应的自测题 ID
    private Long selfTestId;

    // 对应的题目 ID
    private Long questionId;

    // 题目标题，方便前端列表展示
    private String questionTitle;

    // 题目来源题库 ID
    private Long questionBankId;

    // 题目来源题库标题
    private String questionBankTitle;

    // 自测题题干
    private String selfTestContent;

    // 用户最近一次做错时选择的答案
    private String selectedAnswer;

    // 当前正确答案，单选题下就是一个选项标识
    private String correctAnswer;

    // 自测题解析
    private String explanation;

    // 历史累计做错次数
    private Integer wrongCount;

    // 最近一次做错时间
    private LocalDateTime lastWrongTime;
}
