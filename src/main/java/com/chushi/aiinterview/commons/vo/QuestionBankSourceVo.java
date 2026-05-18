package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 题目来源题库的精简信息，用于把题目列表和题库来源做批量合并
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionBankSourceVo {
    // 题目 ID
    private Long questionId;

    // 来源题库 ID
    private Long questionBankId;

    // 来源题库标题
    private String questionBankTitle;
}
