package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.dto.AiChatMessageCreateDto;
import com.chushi.aiinterview.commons.dto.AiChatSessionCreateDto;
import com.chushi.aiinterview.commons.dto.AiChatSessionUpdateDto;
import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.commons.vo.AiChatMessageSendVo;
import com.chushi.aiinterview.commons.vo.AiChatMessageVo;
import com.chushi.aiinterview.commons.vo.AiChatSessionVo;
import com.chushi.aiinterview.commons.vo.QuestionVo;
import com.chushi.aiinterview.components.AiChatModelProvider;
import com.chushi.aiinterview.entities.AiChatMessage;
import com.chushi.aiinterview.entities.AiChatSession;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.AiChatMessageMapper;
import com.chushi.aiinterview.mappers.AiChatSessionMapper;
import com.chushi.aiinterview.services.AiChatService;
import com.chushi.aiinterview.services.QuestionService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {
    private static final String SESSION_STATUS_ACTIVE = "active";

    private static final String SESSION_TITLE_FALLBACK = "AI 对话";

    private static final String SESSION_TITLE_PREFIX = "追问：";

    private static final String MESSAGE_ROLE_USER = "user";

    private static final String MESSAGE_ROLE_ASSISTANT = "assistant";

    private static final String MESSAGE_STATUS_SUCCESS = "success";

    private static final String MESSAGE_STATUS_FAILED = "failed";

    private static final int MAX_SESSION_TITLE_LENGTH = 128;

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1024;

    private static final int MEMORY_MESSAGE_LIMIT = 10;

    @Resource
    private QuestionService questionService;

    @Resource
    private AiChatSessionMapper aiChatSessionMapper;

    @Resource
    private AiChatMessageMapper aiChatMessageMapper;

    @Resource
    private AiChatModelProvider aiChatModelProvider;

    @Resource
    private IdGenerator<Long> idGenerator;

    @Override
    public AiChatSessionVo createQuestionChatSession(Long questionId, AiChatSessionCreateDto request, User currentUser) {
        // 创建会话前先查题目详情：既拿题目标题，也复用题目访问权限校验。
        var question = questionService.getQuestionById(questionId, currentUser);
        var now = TimeUtils.currentLocalDateTime();
        var title = buildSessionTitle(request.getTitle(), question.getTitle());
        var session = AiChatSession.builder()
                .id(idGenerator.nextId())
                .userId(currentUser.getId())
                .questionId(questionId)
                .title(title)
                .status(SESSION_STATUS_ACTIVE)
                .createTime(now)
                .updateTime(now)
                .build();
        aiChatSessionMapper.insert(session);
        return AiChatSessionVo.builder()
                .id(session.getId())
                .questionId(questionId)
                .questionTitle(question.getTitle())
                .title(title)
                .status(session.getStatus())
                .createTime(now)
                .updateTime(now)
                .build();
    }

    @Override
    public List<AiChatSessionVo> getQuestionChatSessionList(Long questionId, User currentUser, Long lastId, Integer size) {
        // 查询会话列表也要校验题目权限，避免通过 questionId 越权查看会话。
        questionService.getQuestionById(questionId, currentUser);
        return aiChatSessionMapper.findSessionListByQuestionId(currentUser.getId(), questionId, lastId, size);
    }


    @Override
    public AiChatSessionVo updateSessionTitle(Long sessionId, AiChatSessionUpdateDto request, User currentUser) {
        var session = getOwnedSession(sessionId, currentUser);
        var now = TimeUtils.currentLocalDateTime();
        var title = limitSessionTitle(request.getTitle().trim());
        aiChatSessionMapper.updateTitle(sessionId, currentUser.getId(), title, now);
        session.setTitle(title);
        session.setUpdateTime(now);
        var question = questionService.getQuestionById(session.getQuestionId(), currentUser);
        return toSessionVo(session, question.getTitle());
    }

    @Override
    public void removeSession(Long sessionId, User currentUser) {
        getOwnedSession(sessionId, currentUser);
        aiChatSessionMapper.softDelete(sessionId, currentUser.getId(), TimeUtils.currentLocalDateTime());
    }

    @Override
    public AiChatMessageSendVo sendMessage(Long sessionId, AiChatMessageCreateDto request, User currentUser) {
        var session = getOwnedSession(sessionId, currentUser);
        var question = questionService.getQuestionById(session.getQuestionId(), currentUser);
        // 先查旧历史，再保存当前用户消息，避免当前问题同时出现在“历史消息”和“当前问题”里。
        var historyMessages = aiChatMessageMapper.findRecentMessagesBySessionId(sessionId, currentUser.getId(), MEMORY_MESSAGE_LIMIT);
        var now = TimeUtils.currentLocalDateTime();
        var userMessage = AiChatMessage.builder()
                .id(idGenerator.nextId())
                .sessionId(sessionId)
                .userId(currentUser.getId())
                .questionId(session.getQuestionId())
                .role(MESSAGE_ROLE_USER)
                .content(request.getContent())
                .status(MESSAGE_STATUS_SUCCESS)
                .createTime(now)
                .updateTime(now)
                .build();
        aiChatMessageMapper.insert(userMessage);
        var sessionUpdateTime = now;
        var autoTitle = buildAutoSessionTitleIfNecessary(session, question, historyMessages, request.getContent());
        if (autoTitle != null) {
            aiChatSessionMapper.updateTitle(sessionId, currentUser.getId(), autoTitle, now);
            session.setTitle(autoTitle);
        } else {
            aiChatSessionMapper.updateTime(sessionId, now);
        }
        session.setUpdateTime(sessionUpdateTime);

        var startNanos = System.nanoTime();
        try {
            // Prompt = 题目上下文 + 最近历史消息 + 当前用户问题。
            var prompt = buildPrompt(question, historyMessages, request.getContent());
            var content = aiChatModelProvider.getChatModel().chat(prompt);
            var assistantMessage = buildAssistantMessage(session, currentUser, content, MESSAGE_STATUS_SUCCESS, null, startNanos);
            aiChatMessageMapper.insert(assistantMessage);
            aiChatSessionMapper.updateTime(sessionId, assistantMessage.getCreateTime());
            return new AiChatMessageSendVo(toMessageVo(userMessage), toMessageVo(assistantMessage));
        } catch (BusinessException e) {
            saveFailedAssistantMessage(session, currentUser, e.getMessage(), startNanos);
            throw e;
        } catch (Exception e) {
            log.error("AiChatMessageException: {}", e.getMessage(), e);
            saveFailedAssistantMessage(session, currentUser, e.getMessage(), startNanos);
            throw new BusinessException(HttpServletResponse.SC_BAD_GATEWAY, "AI service call failed");
        }
    }

    @Override
    public List<AiChatMessageVo> getMessageList(Long sessionId, User currentUser, Long lastId, Integer size) {
        getOwnedSession(sessionId, currentUser);
        var messages = new ArrayList<>(aiChatMessageMapper.findMessageListBySessionId(sessionId, currentUser.getId(), lastId, size));
        // 数据库按 id DESC 取最新一页；返回前反转成时间正序，前端聊天窗口可以直接从上到下渲染。
        Collections.reverse(messages);
        return messages;
    }

    private AiChatSession getOwnedSession(Long sessionId, User currentUser) {
        // sessionId 来自前端，必须确认这个会话属于当前登录用户。
        var session = aiChatSessionMapper.findById(sessionId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "AI chat session not found")
        );
        if (!Objects.equals(session.getUserId(), currentUser.getId())) {
            throw new BusinessException(HttpServletResponse.SC_FORBIDDEN, "AI chat session permission denied");
        }
        return session;
    }

    private AiChatMessage buildAssistantMessage(AiChatSession session,
                                                User currentUser,
                                                String content,
                                                String status,
                                                String errorMessage,
                                                long startNanos) {
        var now = TimeUtils.currentLocalDateTime();
        return AiChatMessage.builder()
                .id(idGenerator.nextId())
                .sessionId(session.getId())
                .userId(currentUser.getId())
                .questionId(session.getQuestionId())
                .role(MESSAGE_ROLE_ASSISTANT)
                .content(content == null ? "" : content)
                .modelName(aiChatModelProvider.getModelName())
                .status(status)
                .errorMessage(limitErrorMessage(errorMessage))
                .latencyMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos))
                .createTime(now)
                .updateTime(now)
                .build();
    }

    private void saveFailedAssistantMessage(AiChatSession session, User currentUser, String errorMessage, long startNanos) {
        try {
            var assistantMessage = buildAssistantMessage(session, currentUser, "", MESSAGE_STATUS_FAILED, errorMessage, startNanos);
            aiChatMessageMapper.insert(assistantMessage);
            aiChatSessionMapper.updateTime(session.getId(), assistantMessage.getCreateTime());
        } catch (Exception recordException) {
            log.warn("AiChatFailedMessageSaveException: {}", recordException.getMessage(), recordException);
        }
    }

    private String buildPrompt(QuestionVo question, List<AiChatMessageVo> historyMessages, String currentUserMessage) {
        return """
                你是一个面向程序员面试刷题场景的 AI 对话助教。
                你只能围绕当前题目和用户的学习问题回答。
                如果用户问题偏离当前题目，请简短回答后引导回当前题目。
                如果题目上下文不足，请明确说明缺少哪些信息，不要编造事实。

                # 当前题目上下文
                题目 ID：%s
                题目标题：%s
                所属题库：%s
                难度：%s
                标签：%s

                # 题目内容
                %s

                # 参考答案
                %s

                # 最近对话历史
                %s

                # 当前用户问题
                %s

                # 输出要求
                - 使用中文回答。
                - 优先服务于面试学习和刷题理解。
                - 回答要承接最近对话历史，但不要机械复述历史。
                - 不要输出与当前题目无关的泛泛内容。
                """.formatted(
                question.getId(),
                safeText(question.getTitle()),
                safeText(question.getQuestionBankTitle()),
                safeText(question.getDifficulty()),
                safeText(question.getTags()),
                safeText(question.getContent()),
                safeText(question.getAnswer()),
                buildHistoryText(historyMessages),
                currentUserMessage
        );
    }

    private String buildHistoryText(List<AiChatMessageVo> historyMessages) {
        // 把数据库中的 user/assistant 消息转换成模型更容易理解的中文对话文本。
        if (historyMessages == null || historyMessages.isEmpty()) {
            return "无";
        }
        var builder = new StringBuilder();
        for (var message : historyMessages) {
            var roleName = MESSAGE_ROLE_USER.equals(message.getRole()) ? "用户" : "助教";
            builder.append(roleName).append("：").append(safeText(message.getContent())).append("\n");
        }
        return builder.toString().trim();
    }

    private AiChatSessionVo toSessionVo(AiChatSession session, String questionTitle) {
        return AiChatSessionVo.builder()
                .id(session.getId())
                .questionId(session.getQuestionId())
                .questionTitle(questionTitle)
                .title(session.getTitle())
                .status(session.getStatus())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    private String buildAutoSessionTitleIfNecessary(AiChatSession session, QuestionVo question, List<AiChatMessageVo> historyMessages, String userContent) {
        if (historyMessages != null && !historyMessages.isEmpty()) {
            return null;
        }
        if (!isDefaultSessionTitle(session.getTitle(), question.getTitle())) {
            return null;
        }
        var normalizedContent = userContent == null ? "" : userContent
                .replaceAll("[\r\n\t]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (!StringUtils.hasText(normalizedContent)) {
            return null;
        }
        return limitSessionTitle(SESSION_TITLE_PREFIX + normalizedContent);
    }

    private boolean isDefaultSessionTitle(String sessionTitle, String questionTitle) {
        if (!StringUtils.hasText(sessionTitle)) {
            return true;
        }
        var title = sessionTitle.trim();
        if (SESSION_TITLE_FALLBACK.equals(title)) {
            return true;
        }
        if (StringUtils.hasText(questionTitle) && title.equals(questionTitle.trim())) {
            return true;
        }
        return StringUtils.hasText(questionTitle) && title.equals("AI 追问：" + questionTitle.trim());
    }

    private String limitSessionTitle(String title) {
        var normalized = StringUtils.hasText(title) ? title.trim() : SESSION_TITLE_FALLBACK;
        return normalized.length() <= MAX_SESSION_TITLE_LENGTH ? normalized : normalized.substring(0, MAX_SESSION_TITLE_LENGTH);
    }

    private AiChatMessageVo toMessageVo(AiChatMessage message) {
        return AiChatMessageVo.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .questionId(message.getQuestionId())
                .role(message.getRole())
                .content(message.getContent())
                .modelName(message.getModelName())
                .status(message.getStatus())
                .errorMessage(message.getErrorMessage())
                .latencyMs(message.getLatencyMs())
                .createTime(message.getCreateTime())
                .build();
    }

    private String buildSessionTitle(String requestTitle, String questionTitle) {
        var title = StringUtils.hasText(requestTitle) ? requestTitle : questionTitle;
        title = StringUtils.hasText(title) ? title : SESSION_TITLE_FALLBACK;
        return limitSessionTitle(title);
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

    private String safeText(Object value) {
        if (value == null) {
            return "无";
        }
        var text = value.toString();
        return StringUtils.hasText(text) ? text : "无";
    }
}
