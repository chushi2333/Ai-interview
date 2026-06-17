package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.configurations.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiRagDecisionServiceImplTest {
    private AiRagDecisionServiceImpl service;
    private RagProperties ragProperties;

    @BeforeEach
    void setUp() {
        service = new AiRagDecisionServiceImpl();
        ragProperties = new RagProperties();
        ReflectionTestUtils.setField(service, "ragProperties", ragProperties);
    }

    @Test
    void decideDisablesRagForShortConfirmationMessages() {
        var decision = service.decide("继续");

        assertThat(decision.getEnabled()).isFalse();
        assertThat(decision.getReason()).isEqualTo("short_message_without_rag_keyword");
        assertThat(decision.getStrategy()).isEqualTo("rule_v1");
    }

    @Test
    void decideEnablesRagWhenMessageMatchesKeyword() {
        var decision = service.decide("HashMap 底层原理是什么");

        assertThat(decision.getEnabled()).isTrue();
        assertThat(decision.getReason()).isEqualTo("matched_keyword:是什么");
        assertThat(decision.getStrategy()).isEqualTo("rule_v1");
    }

    @Test
    void decideReadsThresholdAndKeywordsFromConfiguration() {
        ragProperties.getDecision().setMinMessageLength(100);
        ragProperties.getDecision().setKeywords(List.of("锁升级"));

        assertThat(service.decide("帮我讲讲锁升级").getEnabled()).isTrue();
        assertThat(service.decide("帮我对比线程池参数").getEnabled()).isFalse();
    }
}
