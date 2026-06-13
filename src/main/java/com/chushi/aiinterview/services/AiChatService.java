package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.dto.AiChatMessageCreateDto;
import com.chushi.aiinterview.commons.dto.AiChatSessionCreateDto;
import com.chushi.aiinterview.commons.dto.AiChatSessionUpdateDto;
import com.chushi.aiinterview.commons.vo.AiChatMessageSendVo;
import com.chushi.aiinterview.commons.vo.AiChatMessageVo;
import com.chushi.aiinterview.commons.vo.AiChatSessionVo;
import com.chushi.aiinterview.entities.User;

import java.util.List;

public interface AiChatService {
    AiChatSessionVo createQuestionChatSession(Long questionId, AiChatSessionCreateDto request, User currentUser);

    List<AiChatSessionVo> getQuestionChatSessionList(Long questionId, User currentUser, Long lastId, Integer size);

    AiChatSessionVo updateSessionTitle(Long sessionId, AiChatSessionUpdateDto request, User currentUser);

    void removeSession(Long sessionId, User currentUser);

    AiChatMessageSendVo sendMessage(Long sessionId, AiChatMessageCreateDto request, User currentUser);

    List<AiChatMessageVo> getMessageList(Long sessionId, User currentUser, Long lastId, Integer size);
}
