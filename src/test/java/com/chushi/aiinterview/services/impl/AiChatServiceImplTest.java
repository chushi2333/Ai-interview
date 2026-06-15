package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.vo.AiChatMessageVo;
import com.chushi.aiinterview.entities.AiChatSession;
import com.chushi.aiinterview.entities.AiUserMemory;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.mappers.AiChatMessageMapper;
import com.chushi.aiinterview.mappers.AiChatSessionMapper;
import com.chushi.aiinterview.mappers.AiUserMemoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {
    @Mock
    private AiChatSessionMapper aiChatSessionMapper;

    @Mock
    private AiChatMessageMapper aiChatMessageMapper;

    @Mock
    private AiUserMemoryMapper aiUserMemoryMapper;

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        aiChatService = new AiChatServiceImpl();
        ReflectionTestUtils.setField(aiChatService, "aiChatSessionMapper", aiChatSessionMapper);
        ReflectionTestUtils.setField(aiChatService, "aiChatMessageMapper", aiChatMessageMapper);
        ReflectionTestUtils.setField(aiChatService, "aiUserMemoryMapper", aiUserMemoryMapper);
    }

    @Test
    void getMemoryReturnsPendingSummaryCountWhenRecentWindowIsFull() {
        var sessionId = 100L;
        var user = User.builder().id(7L).build();
        var session = AiChatSession.builder()
                .id(sessionId)
                .userId(user.getId())
                .memorySummary("用户对 HashMap 扩容理解薄弱")
                .summaryMessageId(20L)
                .build();
        var recentMessages = List.of(
                message(31L), message(32L), message(33L), message(34L), message(35L),
                message(36L), message(37L), message(38L), message(39L), message(40L)
        );

        when(aiChatSessionMapper.findById(sessionId)).thenReturn(Optional.of(session));
        when(aiChatMessageMapper.countSuccessMessagesBySessionId(sessionId, user.getId())).thenReturn(16);
        when(aiChatMessageMapper.findRecentMessagesBySessionId(sessionId, user.getId(), 10)).thenReturn(recentMessages);
        when(aiChatMessageMapper.countSummaryMessagesBySessionId(sessionId, user.getId(), 20L, 31L)).thenReturn(4);

        var memory = aiChatService.getMemory(sessionId, user);

        assertThat(memory.getSessionId()).isEqualTo(sessionId);
        assertThat(memory.getMemorySummary()).isEqualTo("用户对 HashMap 扩容理解薄弱");
        assertThat(memory.getSummaryMessageId()).isEqualTo(20L);
        assertThat(memory.getSuccessMessageCount()).isEqualTo(16);
        assertThat(memory.getRecentMessageCount()).isEqualTo(10);
        assertThat(memory.getPendingSummaryMessageCount()).isEqualTo(4);
        assertThat(memory.getSummaryStrategy()).isEqualTo("immediate");
        assertThat(memory.getSummaryTriggerReady()).isTrue();
        assertThat(memory.getSummaryTriggerReason()).isEqualTo("已达到摘要触发条件，下一次发送消息后可刷新长期摘要");
        assertThat(memory.getSummaryTriggerSuccessMessageCount()).isEqualTo(12);
        assertThat(memory.getSummaryRecentMessageReserved()).isEqualTo(10);
        assertThat(memory.getSummaryMinSourceMessageCount()).isEqualTo(1);
        verify(aiChatMessageMapper).countSummaryMessagesBySessionId(sessionId, user.getId(), 20L, 31L);
    }

    @Test
    void getMemoryDoesNotCountPendingSummaryWhenRecentWindowIsNotFull() {
        var sessionId = 101L;
        var user = User.builder().id(8L).build();
        var session = AiChatSession.builder()
                .id(sessionId)
                .userId(user.getId())
                .build();
        var recentMessages = List.of(message(1L), message(2L), message(3L));

        when(aiChatSessionMapper.findById(sessionId)).thenReturn(Optional.of(session));
        when(aiChatMessageMapper.countSuccessMessagesBySessionId(sessionId, user.getId())).thenReturn(3);
        when(aiChatMessageMapper.findRecentMessagesBySessionId(sessionId, user.getId(), 10)).thenReturn(recentMessages);

        var memory = aiChatService.getMemory(sessionId, user);

        assertThat(memory.getRecentMessageCount()).isEqualTo(3);
        assertThat(memory.getPendingSummaryMessageCount()).isZero();
        assertThat(memory.getSummaryStrategy()).isEqualTo("immediate");
        assertThat(memory.getSummaryTriggerReady()).isFalse();
        assertThat(memory.getSummaryTriggerReason()).isEqualTo("成功消息数未达到摘要触发阈值");
        assertThat(memory.getSummaryTriggerSuccessMessageCount()).isEqualTo(12);
        assertThat(memory.getSummaryRecentMessageReserved()).isEqualTo(10);
        assertThat(memory.getSummaryMinSourceMessageCount()).isEqualTo(1);
        verifyNoMoreInteractions(aiChatMessageMapper);
    }

    @Test
    void getCurrentUserMemoryReturnsEmptyStateWhenMemoryDoesNotExist() {
        var user = User.builder().id(9L).build();

        when(aiUserMemoryMapper.findByUserId(user.getId())).thenReturn(Optional.empty());

        var memory = aiChatService.getCurrentUserMemory(user);

        assertThat(memory.getHasMemory()).isFalse();
        assertThat(memory.getMemorySummary()).isNull();
        assertThat(memory.getSourceSessionCount()).isZero();
        assertThat(memory.getLastSourceSessionId()).isNull();
        assertThat(memory.getPromptEnabled()).isTrue();
        assertThat(memory.getUpdateStrategy()).isEqualTo("session_summary");
        assertThat(memory.getMaxMemoryLength()).isEqualTo(3000);
    }

    @Test
    void getCurrentUserMemoryReturnsExistingMemory() {
        var user = User.builder().id(10L).build();
        var now = LocalDateTime.of(2026, 6, 15, 10, 30);
        var storedMemory = AiUserMemory.builder()
                .id(1000L)
                .userId(user.getId())
                .memorySummary("用户在 Java 并发和 MySQL 索引上需要继续巩固")
                .sourceSessionCount(3)
                .lastSourceSessionId(2000L)
                .createTime(now.minusDays(1))
                .updateTime(now)
                .build();

        when(aiUserMemoryMapper.findByUserId(user.getId())).thenReturn(Optional.of(storedMemory));

        var memory = aiChatService.getCurrentUserMemory(user);

        assertThat(memory.getHasMemory()).isTrue();
        assertThat(memory.getMemorySummary()).isEqualTo("用户在 Java 并发和 MySQL 索引上需要继续巩固");
        assertThat(memory.getSourceSessionCount()).isEqualTo(3);
        assertThat(memory.getLastSourceSessionId()).isEqualTo(2000L);
        assertThat(memory.getCreateTime()).isEqualTo(now.minusDays(1));
        assertThat(memory.getUpdateTime()).isEqualTo(now);
        assertThat(memory.getPromptEnabled()).isTrue();
        assertThat(memory.getUpdateStrategy()).isEqualTo("session_summary");
        assertThat(memory.getMaxMemoryLength()).isEqualTo(3000);
    }

    private AiChatMessageVo message(Long id) {
        return AiChatMessageVo.builder().id(id).build();
    }
}
