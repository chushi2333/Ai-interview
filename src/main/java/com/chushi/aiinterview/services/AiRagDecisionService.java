package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.vo.AiRagDecisionVo;

public interface AiRagDecisionService {
    AiRagDecisionVo decide(String userMessage);
}
