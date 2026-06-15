package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.dto.AiChatMessageCreateDto;
import com.chushi.aiinterview.commons.dto.AiChatSessionCreateDto;
import com.chushi.aiinterview.commons.dto.AiChatSessionUpdateDto;
import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.commons.vo.AiChatMessageSendVo;
import com.chushi.aiinterview.commons.vo.AiChatMessageVo;
import com.chushi.aiinterview.commons.vo.AiChatMemoryVo;
import com.chushi.aiinterview.commons.vo.AiChatSessionVo;
import com.chushi.aiinterview.commons.vo.AiUserMemoryVo;
import com.chushi.aiinterview.commons.vo.QuestionVo;
import com.chushi.aiinterview.components.AiChatModelProvider;
import com.chushi.aiinterview.entities.AiChatMessage;
import com.chushi.aiinterview.entities.AiChatSession;
import com.chushi.aiinterview.entities.AiUserMemory;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.AiChatMessageMapper;
import com.chushi.aiinterview.mappers.AiChatSessionMapper;
import com.chushi.aiinterview.mappers.AiUserMemoryMapper;
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

    private static final String SUMMARY_STRATEGY_IMMEDIATE = "immediate";

    private static final String USER_MEMORY_UPDATE_STRATEGY_SESSION_SUMMARY = "session_summary";

    private static final int MAX_SESSION_TITLE_LENGTH = 128;

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1024;

    private static final int MEMORY_MESSAGE_LIMIT = 10;

    // 单条历史消息限制，避免某一次长回答占满整个 Prompt。
    private static final int MAX_HISTORY_MESSAGE_CONTENT_LENGTH = 800;

    // 最近历史总长度限制，避免最近 10 条消息整体过长。
    private static final int MAX_HISTORY_TEXT_LENGTH = 5000;

    // 成功消息达到该数量后才尝试生成长期摘要，避免短对话过早额外调用模型。
    private static final int SUMMARY_TRIGGER_SUCCESS_MESSAGE_COUNT = 12;

    // 最近这部分消息仍作为短期记忆保留，不压进长期摘要。
    private static final int SUMMARY_RECENT_MESSAGE_RESERVED = 10;

    // A 方案：只要有旧消息滑出最近窗口，就用低成本模型刷新摘要，让长期记忆更及时。
    private static final int SUMMARY_MIN_SOURCE_MESSAGE_COUNT = 1;

    // 单次最多拿多少条旧消息做摘要，避免摘要请求本身过大。
    private static final int SUMMARY_SOURCE_MESSAGE_LIMIT = 30;

    private static final int MAX_SUMMARY_SOURCE_TEXT_LENGTH = 8000;

    private static final int MAX_MEMORY_SUMMARY_LENGTH = 3000;

    private static final int MAX_USER_MEMORY_SUMMARY_LENGTH = 3000;

    @Resource
    private QuestionService questionService;

    @Resource
    private AiChatSessionMapper aiChatSessionMapper;

    @Resource
    private AiChatMessageMapper aiChatMessageMapper;

    @Resource
    private AiUserMemoryMapper aiUserMemoryMapper;

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
    public AiChatMemoryVo getMemory(Long sessionId, User currentUser) {
        var session = getOwnedSession(sessionId, currentUser);
        var successMessageCount = aiChatMessageMapper.countSuccessMessagesBySessionId(sessionId, currentUser.getId());
        var recentMessages = aiChatMessageMapper.findRecentMessagesBySessionId(
                sessionId, currentUser.getId(), SUMMARY_RECENT_MESSAGE_RESERVED
        );
        var pendingSummaryMessageCount = 0;
        if (recentMessages.size() >= SUMMARY_RECENT_MESSAGE_RESERVED) {
            // 最近 10 条之前的旧消息才是摘要候选，pending 表示这些候选里还有多少没被 summary_message_id 覆盖。
            var beforeMessageId = recentMessages.get(0).getId();
            pendingSummaryMessageCount = aiChatMessageMapper.countSummaryMessagesBySessionId(
                    sessionId, currentUser.getId(), session.getSummaryMessageId(), beforeMessageId
            );
        }
        // 调试接口直接给出触发判断，复盘时不用再手动对照多个阈值。
        var triggerReady = isSummaryTriggerReady(successMessageCount, recentMessages.size(), pendingSummaryMessageCount);
        return AiChatMemoryVo.builder()
                .sessionId(sessionId)
                .memorySummary(session.getMemorySummary())
                .summaryMessageId(session.getSummaryMessageId())
                .successMessageCount(successMessageCount)
                .recentMessageCount(recentMessages.size())
                .pendingSummaryMessageCount(pendingSummaryMessageCount)
                .summaryStrategy(SUMMARY_STRATEGY_IMMEDIATE)
                .summaryTriggerReady(triggerReady)
                .summaryTriggerReason(buildSummaryTriggerReason(successMessageCount, recentMessages.size(), pendingSummaryMessageCount))
                .summaryTriggerSuccessMessageCount(SUMMARY_TRIGGER_SUCCESS_MESSAGE_COUNT)
                .summaryRecentMessageReserved(SUMMARY_RECENT_MESSAGE_RESERVED)
                .summaryMinSourceMessageCount(SUMMARY_MIN_SOURCE_MESSAGE_COUNT)
                .build();
    }

    private boolean isSummaryTriggerReady(int successMessageCount, int recentMessageCount, int pendingSummaryMessageCount) {
        return successMessageCount >= SUMMARY_TRIGGER_SUCCESS_MESSAGE_COUNT
                && recentMessageCount >= SUMMARY_RECENT_MESSAGE_RESERVED
                && pendingSummaryMessageCount >= SUMMARY_MIN_SOURCE_MESSAGE_COUNT;
    }

    private String buildSummaryTriggerReason(int successMessageCount, int recentMessageCount, int pendingSummaryMessageCount) {
        if (successMessageCount < SUMMARY_TRIGGER_SUCCESS_MESSAGE_COUNT) {
            return "成功消息数未达到摘要触发阈值";
        }
        if (recentMessageCount < SUMMARY_RECENT_MESSAGE_RESERVED) {
            return "最近消息窗口尚未填满，还没有旧消息需要摘要";
        }
        if (pendingSummaryMessageCount < SUMMARY_MIN_SOURCE_MESSAGE_COUNT) {
            return "最近窗口之前没有待摘要旧消息";
        }
        return "已达到摘要触发条件，下一次发送消息后可刷新长期摘要";
    }

    @Override
    public AiUserMemoryVo getCurrentUserMemory(User currentUser) {
        return aiUserMemoryMapper.findByUserId(currentUser.getId())
                .map(memory -> AiUserMemoryVo.builder()
                        .hasMemory(true)
                        .memorySummary(memory.getMemorySummary())
                        .sourceSessionCount(memory.getSourceSessionCount())
                        .lastSourceSessionId(memory.getLastSourceSessionId())
                        .createTime(memory.getCreateTime())
                        .updateTime(memory.getUpdateTime())
                        .promptEnabled(true)
                        .updateStrategy(USER_MEMORY_UPDATE_STRATEGY_SESSION_SUMMARY)
                        .maxMemoryLength(MAX_USER_MEMORY_SUMMARY_LENGTH)
                        .build())
                .orElseGet(() -> AiUserMemoryVo.builder()
                        .hasMemory(false)
                        .sourceSessionCount(0)
                        .promptEnabled(true)
                        .updateStrategy(USER_MEMORY_UPDATE_STRATEGY_SESSION_SUMMARY)
                        .maxMemoryLength(MAX_USER_MEMORY_SUMMARY_LENGTH)
                        .build());
    }

    @Override
    public AiChatMessageSendVo sendMessage(Long sessionId, AiChatMessageCreateDto request, User currentUser) {
        var session = getOwnedSession(sessionId, currentUser);
        var question = questionService.getQuestionById(session.getQuestionId(), currentUser);
        // 先查旧历史，再保存当前用户消息，避免当前问题同时出现在“历史消息”和“当前问题”里。
        var historyMessages = aiChatMessageMapper.findRecentMessagesBySessionId(sessionId, currentUser.getId(), MEMORY_MESSAGE_LIMIT);
        var sessionUpdateTime = TimeUtils.currentLocalDateTime();
        var userMessage = AiChatMessage.builder()
                .id(idGenerator.nextId())
                .sessionId(sessionId)
                .userId(currentUser.getId())
                .questionId(session.getQuestionId())
                .role(MESSAGE_ROLE_USER)
                .content(request.getContent())
                .status(MESSAGE_STATUS_SUCCESS)
                .createTime(sessionUpdateTime)
                .updateTime(sessionUpdateTime)
                .build();
        aiChatMessageMapper.insert(userMessage);
        // 根据对话自动生成标题
        var autoTitle = buildAutoSessionTitleIfNecessary(session, question, historyMessages, request.getContent());
        if (autoTitle != null) {
            aiChatSessionMapper.updateTitle(sessionId, currentUser.getId(), autoTitle, sessionUpdateTime);
            session.setTitle(autoTitle);
        } else {
            aiChatSessionMapper.updateTime(sessionId, sessionUpdateTime);
        }
        session.setUpdateTime(sessionUpdateTime);

        var startNanos = System.nanoTime();
        try {
            // Prompt = 题目上下文 + 用户长期记忆 + 当前会话摘要 + 最近历史消息 + 当前用户问题。
            var userMemorySummary = aiUserMemoryMapper.findByUserId(currentUser.getId())
                    .map(AiUserMemory::getMemorySummary)
                    .orElse(null);
            var prompt = buildPrompt(question, userMemorySummary, session.getMemorySummary(), historyMessages, request.getContent());
            var content = aiChatModelProvider.getChatModel().chat(prompt);
            var assistantMessage = buildAssistantMessage(session, currentUser, content, MESSAGE_STATUS_SUCCESS, null, startNanos);
            aiChatMessageMapper.insert(assistantMessage);
            aiChatSessionMapper.updateTime(sessionId, assistantMessage.getCreateTime());

            tryRefreshMemorySummary(session, question, currentUser);
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

    private String buildPrompt(QuestionVo question,
                               String userMemorySummary,
                               String sessionMemorySummary,
                               List<AiChatMessageVo> historyMessages,
                               String currentUserMessage) {
        return """
                你是一个面向程序员面试刷题场景的 AI 对话助教。
                你只能围绕当前题目和用户的学习问题回答。
                如果用户问题偏离当前题目，请简短回答后引导回当前题目。
                如果题目上下文不足，请明确说明缺少哪些信息，不要编造事实。

                # 当前题目上下文
                题目标题：%s
                所属题库：%s
                难度：%s
                标签：%s

                # 题目内容
                %s

                # 参考答案
                %s

                # 用户长期学习记忆
                %s

                # 当前会话长期摘要
                %s

                # 最近对话历史
                下面是经过裁剪的最近有效对话历史，序号越大表示越接近当前问题。
                %s

                # 当前用户问题
                %s

                # 输出要求
                - 使用中文回答。
                - 优先服务于面试学习和刷题理解。
                - 回答要承接最近对话历史，但不要机械复述历史。
                - 不要输出与当前题目无关的泛泛内容。
                - 不要向用户暴露内部题目 ID、数据库 ID、会话 ID 等系统内部标识。
                """.formatted(
                safeText(question.getTitle()),
                safeText(question.getQuestionBankTitle()),
                safeText(question.getDifficulty()),
                safeText(question.getTags()),
                safeText(question.getContent()),
                safeText(question.getAnswer()),
                buildMemorySummaryText(userMemorySummary),
                buildMemorySummaryText(sessionMemorySummary),
                buildHistoryText(historyMessages),
                currentUserMessage
        );
    }

    private void tryRefreshMemorySummary(AiChatSession session, QuestionVo question, User currentUser) {
        try {
            // 摘要由模型生成，会产生额外调用；短对话先不摘要，避免浪费成本和增加延迟。
            var successMessageCount = aiChatMessageMapper.countSuccessMessagesBySessionId(session.getId(), currentUser.getId());
            if (successMessageCount < SUMMARY_TRIGGER_SUCCESS_MESSAGE_COUNT) {
                return;
            }

            // 最近 N 条仍作为短期记忆原文进入 Prompt，不放进摘要，避免同一内容重复出现。
            var recentMessages = aiChatMessageMapper.findRecentMessagesBySessionId(
                    session.getId(), currentUser.getId(), SUMMARY_RECENT_MESSAGE_RESERVED
            );
            if (recentMessages.size() < SUMMARY_RECENT_MESSAGE_RESERVED) {
                return;
            }

            // 最近 10 条继续作为短期记忆，摘要只处理它们之前的旧消息。
            var beforeMessageId = recentMessages.get(0).getId();
            var summaryMessages = aiChatMessageMapper.findSummaryMessagesBySessionId(
                    session.getId(),
                    currentUser.getId(),
                    session.getSummaryMessageId(),
                    beforeMessageId,
                    SUMMARY_SOURCE_MESSAGE_LIMIT
            );
            if (summaryMessages.isEmpty()) {
                return;
            }

            // 构造长期摘要的提示词
            var summaryPrompt = buildSummaryPrompt(session.getMemorySummary(), summaryMessages);
            // 第二次模型调用：不是回答用户，而是把旧对话压缩成当前会话的长期记忆摘要。
            var newSummary = aiChatModelProvider.getChatModel().chat(summaryPrompt);
            // 记录摘要覆盖到的最后一条消息，下次只摘要更新的旧消息，避免重复压缩。
            var latestSummaryMessageId = summaryMessages.get(summaryMessages.size() - 1).getId();
            var limitedSummary = limitText(newSummary, MAX_MEMORY_SUMMARY_LENGTH);
            aiChatSessionMapper.updateMemorySummary(
                    session.getId(),
                    currentUser.getId(),
                    limitedSummary,
                    latestSummaryMessageId,
                    TimeUtils.currentLocalDateTime()
            );
            session.setMemorySummary(limitedSummary);
            session.setSummaryMessageId(latestSummaryMessageId);
            // session 摘要更新成功后，再把这份高密度摘要合并进用户级长期记忆。
            tryRefreshUserMemory(session, question, currentUser, limitedSummary);
        } catch (Exception e) {
            // 摘要是辅助记忆能力，不能因为摘要模型调用失败导致用户本轮聊天失败。
            log.warn("AiChatMemorySummaryRefreshException: {}", e.getMessage(), e);
        }
    }

    private void tryRefreshUserMemory(AiChatSession session, QuestionVo question, User currentUser, String sessionSummary) {
        try {
            if (!StringUtils.hasText(sessionSummary)) {
                return;
            }
            var existingMemory = aiUserMemoryMapper.findByUserId(currentUser.getId());
            var currentUserMemory = existingMemory.map(AiUserMemory::getMemorySummary).orElse(null);
            var prompt = buildUserMemoryPrompt(currentUserMemory, sessionSummary, question);
            // 第三次模型调用：不是回答用户，而是把会话摘要滚动合并成用户级学习画像。
            var newUserMemory = aiChatModelProvider.getChatModel().chat(prompt);
            var limitedUserMemory = limitText(newUserMemory, MAX_USER_MEMORY_SUMMARY_LENGTH);
            var now = TimeUtils.currentLocalDateTime();
            if (existingMemory.isPresent()) {
                var memory = existingMemory.get();
                var sourceSessionCount = memory.getSourceSessionCount() == null ? 0 : memory.getSourceSessionCount();
                if (!Objects.equals(memory.getLastSourceSessionId(), session.getId())) {
                    sourceSessionCount++;
                }
                aiUserMemoryMapper.updateByUserId(
                        currentUser.getId(),
                        limitedUserMemory,
                        sourceSessionCount,
                        session.getId(),
                        now
                );
                return;
            }

            aiUserMemoryMapper.insert(AiUserMemory.builder()
                    .id(idGenerator.nextId())
                    .userId(currentUser.getId())
                    .memorySummary(limitedUserMemory)
                    .sourceSessionCount(1)
                    .lastSourceSessionId(session.getId())
                    .createTime(now)
                    .updateTime(now)
                    .build());
        } catch (Exception e) {
            // 用户长期记忆是辅助画像能力，失败不能影响本轮聊天和 session 摘要。
            log.warn("AiUserMemoryRefreshException: {}", e.getMessage(), e);
        }
    }

    // 用户记忆 Prompt 基于 session 摘要，而不是原始消息，降低 token 和模型调用成本。
    private String buildUserMemoryPrompt(String currentUserMemory, String sessionSummary, QuestionVo question) {
        return """
                你是一个面向程序员面试刷题场景的用户学习画像摘要器。
                请基于已有用户长期记忆和本次会话摘要，输出更新后的用户长期学习记忆。

                # 已有用户长期记忆
                %s

                # 本次会话摘要
                %s

                # 本次会话题目信息
                题目标题：%s
                所属题库：%s
                难度：%s
                标签：%s

                # 摘要要求
                - 使用中文。
                - 只记录对后续面试学习有长期价值的信息。
                - 保留用户反复暴露的薄弱点、偏好的解释方式、尚未解决的问题。
                - 不记录手机号、邮箱、密钥、验证码等隐私或敏感信息。
                - 不记录内部题目 ID、数据库 ID、会话 ID、消息 ID。
                - 不逐字复述本次会话摘要。
                - 控制在 1000 字以内。
                """.formatted(
                buildMemorySummaryText(currentUserMemory),
                safeText(sessionSummary),
                safeText(question.getTitle()),
                safeText(question.getQuestionBankTitle()),
                safeText(question.getDifficulty()),
                safeText(question.getTags())
        );
    }

    // 摘要 Prompt 和聊天 Prompt 分开：这里要求模型压缩记忆，不要求回答用户问题。
    private String buildSummaryPrompt(String currentSummary, List<AiChatMessageVo> summaryMessages) {
        return """
                你是一个对话记忆摘要器，负责把程序员面试刷题对话压缩成长期记忆。
                请基于已有摘要和新增对话，输出一段更新后的长期记忆摘要。

                # 已有长期摘要
                %s

                # 新增待摘要对话
                %s

                # 摘要要求
                - 使用中文。
                - 只保留对后续学习和追问有帮助的信息。
                - 保留用户暴露出的薄弱点、已经解释过的关键结论、尚未解决的问题。
                - 不要记录内部题目 ID、数据库 ID、会话 ID、消息 ID。
                - 不要逐字复述对话。
                - 控制在 800 字以内。
                """.formatted(
                buildMemorySummaryText(currentSummary),
                buildSummarySourceText(summaryMessages)
        );
    }

    private String buildSummarySourceText(List<AiChatMessageVo> summaryMessages) {
        if (summaryMessages == null || summaryMessages.isEmpty()) {
            return "无";
        }

        var lines = new ArrayList<String>();
        for (var message : summaryMessages) {
            var content = normalizeHistoryContent(message.getContent());
            if (!StringUtils.hasText(content)) {
                continue;
            }
            var roleName = MESSAGE_ROLE_USER.equals(message.getRole()) ? "用户" : "助教";
            lines.add(roleName + "：" + limitText(content, MAX_HISTORY_MESSAGE_CONTENT_LENGTH));
        }
        return limitText(String.join("\n", lines), MAX_SUMMARY_SOURCE_TEXT_LENGTH);
    }

    private String buildMemorySummaryText(String memorySummary) {
        return StringUtils.hasText(memorySummary) ? memorySummary.trim() : "无";
    }

    private String buildHistoryText(List<AiChatMessageVo> historyMessages) {
        // 把数据库中的 user/assistant 消息转换成模型更容易理解的中文对话文本，同时限制 prompt 体积。
        if (historyMessages == null || historyMessages.isEmpty()) {
            return "无";
        }

        var normalizedLines = new ArrayList<String>();
        // Mapper 已经做了 status/content 过滤，这里再做一次防御，防止后续 SQL 调整影响 Prompt 质量。
        for (var message : historyMessages) {
            if (!MESSAGE_STATUS_SUCCESS.equals(message.getStatus())) {
                continue;
            }
            var content = normalizeHistoryContent(message.getContent());
            if (!StringUtils.hasText(content)) {
                continue;
            }
            var roleName = MESSAGE_ROLE_USER.equals(message.getRole()) ? "用户" : "助教";
            // 先限制单条消息长度，再进入总长度选择。
            normalizedLines.add(roleName + "：" + limitText(content, MAX_HISTORY_MESSAGE_CONTENT_LENGTH));
        }
        if (normalizedLines.isEmpty()) {
            return "无";
        }

        var selectedLines = new ArrayList<String>();
        var totalLength = 0;
        // 从最新消息往前选择，历史太长时优先保留离当前问题更近的上下文。
        var omitted = false;
        for (var index = normalizedLines.size() - 1; index >= 0; index--) {
            var line = normalizedLines.get(index);
            var nextLength = totalLength + line.length() + 8;
            if (nextLength > MAX_HISTORY_TEXT_LENGTH) {
                omitted = true;
                break;
            }
            selectedLines.add(line);
            totalLength = nextLength;
        }
        // 上面是从新到旧选出来的；返回给模型前再反转，保持真实对话顺序。
        Collections.reverse(selectedLines);

        var builder = new StringBuilder();
        if (omitted) {
            builder.append("部分较早历史已因长度限制省略。\n");
        }
        for (var index = 0; index < selectedLines.size(); index++) {
            builder.append(index + 1).append(". ").append(selectedLines.get(index));
            if (index < selectedLines.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString().trim();
    }

    // 历史消息进入 Prompt 前压成单行，减少换行和多余空白占用 token。
    private String normalizeHistoryContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        return content
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // 超长内容保留前半部分，并显式告诉模型这条历史被截断过。
    private String limitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...（已截断）";
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
