package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.dto.QuestionBankCreateDto;
import com.chushi.aiinterview.commons.dto.QuestionBankUpdateDto;
import com.chushi.aiinterview.entities.QuestionBank;
import com.chushi.aiinterview.entities.User;

import java.util.List;

// 题库业务接口
public interface QuestionBankService {
    QuestionBank createQuestionBank(User creator, QuestionBankCreateDto questionBank);

    QuestionBank getQuestionBankById(Long questionBankId);

    List<QuestionBank> getQuestionBankList(Long cursor, Integer limit, Long userId);

    void updateQuestionBank(Long questionBankId, Long userId, QuestionBankUpdateDto questionBank);

    QuestionBank updateQuestionBankPicture(Long questionBankId, String picture);

    void removeQuestionBank(Long questionBankId, Long userId);
}
