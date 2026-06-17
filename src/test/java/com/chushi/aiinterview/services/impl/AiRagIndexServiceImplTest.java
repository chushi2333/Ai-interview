package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.vo.AiRagQuestionIndexVo;
import com.chushi.aiinterview.configurations.RagProperties;
import com.chushi.aiinterview.entities.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class AiRagIndexServiceImplTest {
    private final AiRagIndexServiceImpl service = new AiRagIndexServiceImpl();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "ragProperties", new RagProperties());
    }

    @Test
    void buildQuestionChunksSplitsLongAnswerAndKeepsQuestionTitle() {
        var longAnswer = "A".repeat(1300) + "B".repeat(1300);
        var question = Question.builder()
                .id(1L)
                .title("HashMap 扩容机制")
                .content("请解释 HashMap 扩容")
                .answer(longAnswer)
                .tags("[\"Java\",\"集合\"]")
                .difficulty(2)
                .build();

        var chunks = service.buildQuestionChunks(question);

        assertThat(chunks).hasSizeGreaterThan(3);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk).contains("# 题目标题\nHashMap 扩容机制"));
        assertThat(chunks).anySatisfy(chunk -> assertThat(chunk).contains("# 参考答案"));
        assertThat(chunks).anySatisfy(chunk -> assertThat(chunk).contains("# 标签"));
        assertThat(chunks).anySatisfy(chunk -> assertThat(chunk).contains("# 难度\n中等"));
    }

    @Test
    void rebuildQuestionIndexBatchIndexesDistinctQuestionIdsAndKeepsFailures() {
        var batchService = new AiRagIndexServiceImpl() {
            @Override
            AiRagQuestionIndexVo rebuildSingleQuestionIndex(Long questionId) {
                if (questionId == 2L) {
                    throw new IllegalStateException("embedding failed");
                }
                return AiRagQuestionIndexVo.builder()
                        .questionId(questionId)
                        .indexedChunkCount(1)
                        .build();
            }
        };
        ReflectionTestUtils.setField(batchService, "ragProperties", new RagProperties());

        var result = batchService.rebuildQuestionIndexBatch(Arrays.asList(1L, 2L, 1L, null), 10);

        assertThat(result.getRequestedCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getItems()).extracting("questionId").containsExactly(1L, 2L);
        assertThat(result.getItems().get(0).getSuccess()).isTrue();
        assertThat(result.getItems().get(1).getSuccess()).isFalse();
        assertThat(result.getItems().get(1).getMessage()).isEqualTo("embedding failed");
    }

    @Test
    void rebuildQuestionIndexBatchLoadsRecentQuestionsWhenIdsAreEmpty() {
        var questionMapper = Mockito.mock(com.chushi.aiinterview.mappers.QuestionMapper.class);
        when(questionMapper.findQuestionList(null, 2, null)).thenReturn(List.of(
                Question.builder().id(10L).build(),
                Question.builder().id(9L).build()
        ));
        var batchService = new AiRagIndexServiceImpl() {
            @Override
            AiRagQuestionIndexVo rebuildSingleQuestionIndex(Long questionId) {
                return AiRagQuestionIndexVo.builder()
                        .questionId(questionId)
                        .indexedChunkCount(1)
                        .build();
            }
        };
        ReflectionTestUtils.setField(batchService, "ragProperties", new RagProperties());
        ReflectionTestUtils.setField(batchService, "questionMapper", questionMapper);

        var result = batchService.rebuildQuestionIndexBatch(List.of(), 2);

        assertThat(result.getRequestedCount()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getItems()).extracting("questionId").containsExactly(10L, 9L);
    }

    @Test
    void splitTextKeepsOverlapBetweenAdjacentSegments() {
        var text = "0123456789abcdefghij";

        var segments = service.splitText(text, 10, 3);

        assertThat(segments).containsExactly("0123456789", "789abcdefg", "efghij");
    }
}
