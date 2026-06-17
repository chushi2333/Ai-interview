package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.vo.AiRagDecisionVo;
import com.chushi.aiinterview.configurations.RagProperties;
import com.chushi.aiinterview.services.AiRagDecisionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiRagDecisionServiceImpl implements AiRagDecisionService {
    private static final String STRATEGY_RULE_V1 = "rule_v1";

    @Resource
    private RagProperties ragProperties;

    @Override
    public AiRagDecisionVo decide(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return disabled("empty_message");
        }

        var normalized = userMessage.trim();
        var decision = ragProperties.getDecision();
        var minMessageLength = safeMinMessageLength(decision.getMinMessageLength());
        // 长问题通常包含明确知识诉求，先用长度规则兜底，避免只靠关键词漏掉自然表达。
        if (normalized.length() >= minMessageLength) {
            return enabled("message_length_gte:" + minMessageLength);
        }

        // 短问题只有命中知识类关键词才触发 RAG，避免“好的 / 继续”这类消息浪费 embedding 调用。
        for (var keyword : decision.getKeywords()) {
            if (StringUtils.hasText(keyword) && normalized.contains(keyword.trim())) {
                return enabled("matched_keyword:" + keyword.trim());
            }
        }

        return disabled("short_message_without_rag_keyword");
    }

    private int safeMinMessageLength(Integer minMessageLength) {
        return minMessageLength == null || minMessageLength < 1 ? 18 : minMessageLength;
    }

    private AiRagDecisionVo enabled(String reason) {
        return AiRagDecisionVo.builder()
                .enabled(true)
                .reason(reason)
                .strategy(STRATEGY_RULE_V1)
                .build();
    }

    private AiRagDecisionVo disabled(String reason) {
        return AiRagDecisionVo.builder()
                .enabled(false)
                .reason(reason)
                .strategy(STRATEGY_RULE_V1)
                .build();
    }
}
