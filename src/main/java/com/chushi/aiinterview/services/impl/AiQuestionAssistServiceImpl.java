package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.dto.AiQuestionAssistRequestDto;
import com.chushi.aiinterview.commons.enums.AiQuestionAssistType;
import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.commons.vo.AiAssistRecordVo;
import com.chushi.aiinterview.commons.vo.AiQuestionAssistVo;
import com.chushi.aiinterview.commons.vo.QuestionVo;
import com.chushi.aiinterview.components.AiChatModelProvider;
import com.chushi.aiinterview.entities.AiAssistRecord;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.AiAssistRecordMapper;
import com.chushi.aiinterview.services.AiQuestionAssistService;
import com.chushi.aiinterview.services.QuestionService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AiQuestionAssistServiceImpl implements AiQuestionAssistService {
    private static final String RECORD_STATUS_SUCCESS = "success";

    private static final String RECORD_STATUS_FAILED = "failed";

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1024;

    @Resource
    private QuestionService questionService;

    @Resource
    private AiChatModelProvider aiChatModelProvider;

    @Resource
    private AiAssistRecordMapper aiAssistRecordMapper;

    @Resource
    private IdGenerator<Long> idGenerator;

    @Override
    public AiQuestionAssistVo assistQuestion(Long questionId, AiQuestionAssistRequestDto request, User currentUser) {
        var assistType = AiQuestionAssistType.fromValue(request.getType());
        var question = questionService.getQuestionById(questionId, currentUser);
        var prompt = buildPrompt(question, assistType, request.getUserInput());
        var startNanos = System.nanoTime();

        try {
            var content = aiChatModelProvider.getChatModel().chat(prompt);
            recordAiAssist(questionId, request, assistType, currentUser, content, RECORD_STATUS_SUCCESS, null, startNanos);
            return new AiQuestionAssistVo(content);
        } catch (BusinessException e) {
            recordAiAssist(questionId, request, assistType, currentUser, null, RECORD_STATUS_FAILED, e.getMessage(), startNanos);
            throw e;
        } catch (Exception e) {
            log.error("AiQuestionAssistException: {}", e.getMessage(), e);
            recordAiAssist(questionId, request, assistType, currentUser, null, RECORD_STATUS_FAILED, e.getMessage(), startNanos);
            throw new BusinessException(HttpServletResponse.SC_BAD_GATEWAY, "AI service call failed");
        }
    }

    @Override
    public List<AiAssistRecordVo> getQuestionAssistRecordList(Long questionId, User currentUser, Long lastId, Integer size) {
        questionService.getQuestionById(questionId, currentUser);
        return aiAssistRecordMapper.findRecordListByQuestionId(currentUser.getId(), questionId, lastId, size);
    }

    private void recordAiAssist(Long questionId,
                                AiQuestionAssistRequestDto request,
                                AiQuestionAssistType assistType,
                                User currentUser,
                                String content,
                                String status,
                                String errorMessage,
                                long startNanos) {
        try {
            var now = TimeUtils.currentLocalDateTime();
            var latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            var record = AiAssistRecord.builder()
                    .id(idGenerator.nextId())
                    .userId(currentUser.getId())
                    .questionId(questionId)
                    .assistType(assistType.getValue())
                    .userInput(request.getUserInput())
                    .content(content)
                    .modelName(aiChatModelProvider.getModelName())
                    .status(status)
                    .errorMessage(limitErrorMessage(errorMessage))
                    .latencyMs(latencyMs)
                    .createTime(now)
                    .updateTime(now)
                    .build();
            aiAssistRecordMapper.insert(record);
        } catch (Exception e) {
            log.warn("AiAssistRecordSaveException: {}", e.getMessage(), e);
        }
    }

    private String limitErrorMessage(String errorMessage) {
        if (!StringUtils.hasText(errorMessage)) {
            return null;
        }
        if (errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private String buildPrompt(QuestionVo question, AiQuestionAssistType assistType, String userInput) {
        return """
                你是一个面向程序员面试刷题场景的 AI 题解助教。
                你只能基于下面给定的题目上下文回答，不要脱离当前题目泛泛而谈。
                如果题目上下文不足，请明确说明缺少哪些信息，不要编造事实。

                # 任务
                %s

                # 题目上下文
                题目 ID：%s
                题目标题：%s
                所属题库：%s
                难度：%s
                标签：%s

                # 题目内容
                %s

                # 参考答案
                %s

                # 用户补充输入
                %s

                # 输出要求
                - 使用中文回答。
                - 内容要服务于面试学习和刷题理解。
                - 回答要具体，不要只给空泛建议。
                - 不要输出与当前题目无关的内容。
                %s
                """.formatted(
                assistType.getDescription(),
                question.getId(),
                safeText(question.getTitle()),
                safeText(question.getQuestionBankTitle()),
                safeText(question.getDifficulty()),
                safeText(question.getTags()),
                safeText(question.getContent()),
                safeText(question.getAnswer()),
                StringUtils.hasText(userInput) ? userInput : "无",
                outputInstruction(assistType)
        );
    }

    private String outputInstruction(AiQuestionAssistType assistType) {
        return switch (assistType) {
            case SIMPLE_EXPLAIN -> "- 用大白话解释核心概念，并给一个贴近开发场景的例子。";
            case INTERVIEW_ANSWER -> "- 按“简短结论、核心原理、实际场景、注意点”的结构组织面试回答。";
            case KEY_POINTS -> "- 用列表提炼 3 到 6 个回答重点，每个重点给一句解释。";
            case FOLLOW_UP_QUESTIONS -> "- 生成 5 个面试官可能继续追问的问题，并标注考察点。";
            case ANSWER_POLISH -> "- 先指出用户回答的问题，再给出一版更适合面试表达的改写。";
        };
    }

    private String safeText(Object value) {
        if (value == null) {
            return "无";
        }
        var text = value.toString();
        return StringUtils.hasText(text) ? text : "无";
    }
}
