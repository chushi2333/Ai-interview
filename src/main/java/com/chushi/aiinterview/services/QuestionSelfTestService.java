package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.dto.QuestionSelfTestCreateDto;
import com.chushi.aiinterview.commons.dto.QuestionSelfTestSubmitDto;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestListVo;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestManageVo;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestSubmitResultVo;
import com.chushi.aiinterview.entities.User;

public interface QuestionSelfTestService {
    QuestionSelfTestManageVo createSelfTest(Long questionId, QuestionSelfTestCreateDto selfTest);

    QuestionSelfTestListVo getSelfTestsByQuestionId(Long questionId);

    QuestionSelfTestSubmitResultVo submitSelfTest(Long selfTestId, User currentUser, QuestionSelfTestSubmitDto submitDto);

    void removeSelfTest(Long selfTestId);
}
