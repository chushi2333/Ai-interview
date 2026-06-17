package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.dto.QuestionCreateDto;
import com.chushi.aiinterview.commons.dto.QuestionUpdateDto;
import com.chushi.aiinterview.commons.vo.QuestionVo;
import com.chushi.aiinterview.entities.Question;
import com.chushi.aiinterview.entities.User;

import java.util.List;

// 题目业务接口
public interface QuestionService {
    Question createQuestion(User creator, QuestionCreateDto question);

    QuestionVo getQuestionById(Long questionId, User currentUser);

    QuestionVo getQuestionPreviewById(Long questionId);

    List<Question> getQuestionList(Long cursor, Integer limit, Long userId);

    List<Question> getQuestionListByQuestionBankId(Long questionBankId, Long cursor, Integer limit);

    void updateQuestion(Long questionId, QuestionUpdateDto question);

    void removeQuestion(Long questionId);
}
