package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.dto.AiQuestionAssistRequestDto;
import com.chushi.aiinterview.commons.vo.AiAssistRecordVo;
import com.chushi.aiinterview.commons.vo.AiQuestionAssistVo;
import com.chushi.aiinterview.entities.User;

import java.util.List;

public interface AiQuestionAssistService {
    AiQuestionAssistVo assistQuestion(Long questionId, AiQuestionAssistRequestDto request, User currentUser);

    List<AiAssistRecordVo> getQuestionAssistRecordList(Long questionId, User currentUser, Long lastId, Integer size);
}
