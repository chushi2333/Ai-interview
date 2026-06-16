package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.commons.vo.AiRagQuestionBatchIndexItemVo;
import com.chushi.aiinterview.commons.vo.AiRagQuestionBatchIndexVo;
import com.chushi.aiinterview.commons.vo.AiRagQuestionIndexVo;
import com.chushi.aiinterview.commons.vo.AiRagSearchDebugVo;
import com.chushi.aiinterview.components.AiEmbeddingModelProvider;
import com.chushi.aiinterview.configurations.RagProperties;
import com.chushi.aiinterview.entities.AiRagChunk;
import com.chushi.aiinterview.entities.Question;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.QuestionMapper;
import com.chushi.aiinterview.rag.mappers.AiRagChunkMapper;
import com.chushi.aiinterview.services.AiRagIndexService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiRagIndexServiceImpl implements AiRagIndexService {
    private static final String SOURCE_TYPE_QUESTION = "question";
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 10;
    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private AiRagChunkMapper aiRagChunkMapper;

    @Resource
    private AiEmbeddingModelProvider aiEmbeddingModelProvider;

    @Resource
    private RagProperties ragProperties;

    @Resource
    private IdGenerator<Long> idGenerator;

    @Override
    public AiRagQuestionIndexVo rebuildQuestionIndex(Long questionId) {
        var question = questionMapper.findById(questionId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question not found")
        );

        var chunkContents = buildQuestionChunks(question);
        if (chunkContents.isEmpty()) {
            throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "Question content is empty for RAG indexing");
        }

        var now = TimeUtils.currentLocalDateTime();
        var metadata = buildMetadata(question);
        // 重建索引要先删旧 chunk，再写新 chunk，避免同一道题重复召回多个旧版本。
        var deletedCount = aiRagChunkMapper.deleteByQuestionId(questionId);
        var totalContentLength = 0;
        for (var index = 0; index < chunkContents.size(); index++) {
            var chunkContent = chunkContents.get(index);
            totalContentLength += chunkContent.length();
            // 每个 chunk 单独 embedding，检索时才能命中题目里的具体知识片段。
            var embedding = aiEmbeddingModelProvider.embedAsPgVector(chunkContent);
            var chunk = AiRagChunk.builder()
                    .id(idGenerator.nextId())
                    .questionId(question.getId())
                    .chunkIndex(index)
                    .sourceType(SOURCE_TYPE_QUESTION)
                    .title(question.getTitle())
                    .content(chunkContent)
                    .embedding(embedding)
                    .metadata(metadata)
                    .createTime(now)
                    .updateTime(now)
                    .build();
            aiRagChunkMapper.insert(chunk);
        }

        return AiRagQuestionIndexVo.builder()
                .questionId(questionId)
                .deletedChunkCount(deletedCount)
                .indexedChunkCount(chunkContents.size())
                .chunkContentLength(totalContentLength)
                .embeddingModelName(aiEmbeddingModelProvider.getModelName())
                .embeddingDimension(aiEmbeddingModelProvider.getConfiguredDimension())
                .dimensionMatched(true)
                .build();
    }


    @Override
    public AiRagQuestionBatchIndexVo rebuildQuestionIndexBatch(List<Long> questionIds, Integer limit) {
        var targetQuestionIds = resolveBatchQuestionIds(questionIds, limit);
        var items = new ArrayList<AiRagQuestionBatchIndexItemVo>();
        var successCount = 0;

        for (var questionId : targetQuestionIds) {
            try {
                // 批量索引复用单题索引逻辑，避免 chunk 切分、embedding、写库规则出现两套实现。
                var result = rebuildSingleQuestionIndex(questionId);
                items.add(AiRagQuestionBatchIndexItemVo.builder()
                        .questionId(questionId)
                        .success(true)
                        .message("success")
                        .result(result)
                        .build());
                successCount++;
            } catch (Exception e) {
                items.add(AiRagQuestionBatchIndexItemVo.builder()
                        .questionId(questionId)
                        .success(false)
                        .message(e.getMessage())
                        .build());
            }
        }

        return AiRagQuestionBatchIndexVo.builder()
                .requestedCount(targetQuestionIds.size())
                .successCount(successCount)
                .failedCount(targetQuestionIds.size() - successCount)
                .items(items)
                .build();
    }


    AiRagQuestionIndexVo rebuildSingleQuestionIndex(Long questionId) {
        return rebuildQuestionIndex(questionId);
    }

    @Override
    public AiRagSearchDebugVo searchDebug(String query, Integer topK) {
        if (!StringUtils.hasText(query)) {
            throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "RAG query must not be empty");
        }

        // 检索阶段和索引阶段必须使用同一个 embedding 模型，否则向量空间不一致，召回结果会失真。
        var queryEmbedding = aiEmbeddingModelProvider.embedAsPgVector(query);
        var normalizedTopK = normalizeTopK(topK);
        var chunks = aiRagChunkMapper.searchTopK(queryEmbedding, normalizedTopK).stream()
                // distance 越小越相似；超过阈值的 chunk 不进入 Prompt，避免低相关资料干扰回答。
                .filter(chunk -> chunk.getDistance() != null && chunk.getDistance() <= maxSearchDistance())
                .toList();

        return AiRagSearchDebugVo.builder()
                .query(query)
                .topK(normalizedTopK)
                .embeddingModelName(aiEmbeddingModelProvider.getModelName())
                .embeddingDimension(aiEmbeddingModelProvider.getConfiguredDimension())
                .matchedChunkCount(chunks.size())
                .chunks(chunks)
                .build();
    }


    private List<Long> resolveBatchQuestionIds(List<Long> questionIds, Integer limit) {
        if (questionIds != null && !questionIds.isEmpty()) {
            return questionIds.stream()
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .limit(50)
                    .toList();
        }

        var normalizedLimit = limit == null ? 10 : Math.min(Math.max(limit, 1), 50);
        return questionMapper.findQuestionList(null, normalizedLimit, null).stream()
                .map(Question::getId)
                .toList();
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        return Math.min(Math.max(topK, 1), MAX_TOP_K);
    }

    List<String> buildQuestionChunks(Question question) {
        var commonContext = buildCommonQuestionContext(question);
        var chunks = new ArrayList<String>();
        addFieldChunks(chunks, commonContext, "题目内容", question.getContent());
        addFieldChunks(chunks, commonContext, "参考答案", question.getAnswer());
        addFieldChunks(chunks, commonContext, "标签", question.getTags());
        addFieldChunks(chunks, commonContext, "难度", difficultyName(question.getDifficulty()));
        if (chunks.isEmpty() && StringUtils.hasText(question.getTitle())) {
            chunks.add(commonContext);
        }
        return chunks;
    }

    private String buildCommonQuestionContext(Question question) {
        var builder = new StringBuilder();
        appendSection(builder, "题目标题", question.getTitle());
        return builder.toString().trim();
    }

    private void addFieldChunks(List<String> chunks, String commonContext, String sectionTitle, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        for (var segment : splitText(content.trim(), chunkMaxLength(), chunkOverlapLength())) {
            var builder = new StringBuilder();
            if (StringUtils.hasText(commonContext)) {
                builder.append(commonContext).append("\n\n");
            }
            appendSection(builder, sectionTitle, segment);
            chunks.add(builder.toString().trim());
        }
    }

    List<String> splitText(String text, int maxLength, int overlapLength) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        if (text.length() <= maxLength) {
            return List.of(text.trim());
        }

        var segments = new ArrayList<String>();
        var start = 0;
        while (start < text.length()) {
            var end = Math.min(start + maxLength, text.length());
            segments.add(text.substring(start, end).trim());
            if (end == text.length()) {
                break;
            }
            // 保留少量 overlap，避免关键解释正好落在两个 chunk 的边界上被切断。
            start = Math.max(end - overlapLength, start + 1);
        }
        return segments;
    }


    private int chunkMaxLength() {
        var maxLength = ragProperties.getChunk().getMaxLength();
        return maxLength == null || maxLength < 1 ? 1200 : maxLength;
    }

    private int chunkOverlapLength() {
        var overlapLength = ragProperties.getChunk().getOverlapLength();
        var maxLength = chunkMaxLength();
        if (overlapLength == null || overlapLength < 0) {
            return 0;
        }
        return Math.min(overlapLength, maxLength - 1);
    }

    private double maxSearchDistance() {
        var maxDistance = ragProperties.getSearch().getMaxDistance();
        return maxDistance == null || maxDistance <= 0 ? 0.45 : maxDistance;
    }

    private void appendSection(StringBuilder builder, String title, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append("\n\n");
        }
        builder.append("# ").append(title).append("\n").append(content.trim());
    }

    private String difficultyName(Integer difficulty) {
        if (difficulty == null) {
            return null;
        }
        return switch (difficulty) {
            case 1 -> "简单";
            case 2 -> "中等";
            case 3 -> "困难";
            default -> String.valueOf(difficulty);
        };
    }

    private String buildMetadata(Question question) {
        return "{\"difficulty\":" + (question.getDifficulty() == null ? "null" : question.getDifficulty()) + "}";
    }
}
