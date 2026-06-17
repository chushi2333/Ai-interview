package com.chushi.aiinterview.services;

import java.util.List;

// 题库题目关系业务接口
public interface QuestionBankQuestionService {
    void addQuestionToBank(Long questionBankId, Long questionId, Long userId);

    int addQuestionsToBank(Long questionBankId, List<Long> questionIds, Long userId);

    void removeQuestionFromBank(Long questionBankId, Long questionId, Long userId);
}
