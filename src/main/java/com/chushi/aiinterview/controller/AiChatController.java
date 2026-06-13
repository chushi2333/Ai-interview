package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.dto.AiChatMessageCreateDto;
import com.chushi.aiinterview.commons.dto.AiChatSessionCreateDto;
import com.chushi.aiinterview.commons.dto.AiChatSessionUpdateDto;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.vo.AiChatMessageListVo;
import com.chushi.aiinterview.commons.vo.AiChatMessageSendVo;
import com.chushi.aiinterview.commons.vo.AiChatSessionListVo;
import com.chushi.aiinterview.commons.vo.AiChatSessionVo;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiChatController extends BaseController {
    @Resource
    private AiChatService aiChatService;

    @PostMapping("/api/ai/question/{questionId}/chat/sessions")
    @Operation(summary = "创建题目 AI 对话会话")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<AiChatSessionVo> createQuestionChatSession(
            @PathVariable Long questionId,
            @Valid @RequestBody AiChatSessionCreateDto request,
            @CurrentUser User currentUser
    ) {
        return wrap(aiChatService.createQuestionChatSession(questionId, request, currentUser));
    }

    @GetMapping("/api/ai/question/{questionId}/chat/sessions")
    @Operation(summary = "获取题目 AI 对话会话列表")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<AiChatSessionListVo> getQuestionChatSessionList(
            @PathVariable Long questionId,
            @Parameter(description = "分页游标，表示上一条 AI 对话会话 ID")
            @PositiveOrZero(message = "Last ID must be >= 0")
            @RequestParam(name = "last_id", required = false) Long lastId,
            @Parameter(description = "每页数量，范围 1-50")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 50, message = "Size must be <= 50")
            @RequestParam(defaultValue = "10") Integer size,
            @CurrentUser User currentUser
    ) {
        return wrap(new AiChatSessionListVo(
                aiChatService.getQuestionChatSessionList(questionId, currentUser, lastId, size)
        ));
    }



    @PutMapping("/api/ai/chat/sessions/{sessionId}")
    @Operation(summary = "修改 AI 对话会话标题")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<AiChatSessionVo> updateSessionTitle(
            @PathVariable Long sessionId,
            @Valid @RequestBody AiChatSessionUpdateDto request,
            @CurrentUser User currentUser
    ) {
        return wrap(aiChatService.updateSessionTitle(sessionId, request, currentUser));
    }

    @DeleteMapping("/api/ai/chat/sessions/{sessionId}")
    @Operation(summary = "删除 AI 对话会话")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<Void> removeSession(
            @PathVariable Long sessionId,
            @CurrentUser User currentUser
    ) {
        aiChatService.removeSession(sessionId, currentUser);
        return wrap();
    }

    @PostMapping("/api/ai/chat/sessions/{sessionId}/messages")
    @Operation(summary = "发送 AI 对话消息")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<AiChatMessageSendVo> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody AiChatMessageCreateDto request,
            @CurrentUser User currentUser
    ) {
        return wrap(aiChatService.sendMessage(sessionId, request, currentUser));
    }

    @GetMapping("/api/ai/chat/sessions/{sessionId}/messages")
    @Operation(summary = "获取 AI 对话消息列表")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<AiChatMessageListVo> getMessageList(
            @PathVariable Long sessionId,
            @Parameter(description = "分页游标，表示上一条 AI 对话消息 ID")
            @PositiveOrZero(message = "Last ID must be >= 0")
            @RequestParam(name = "last_id", required = false) Long lastId,
            @Parameter(description = "每页数量，范围 1-50")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 50, message = "Size must be <= 50")
            @RequestParam(defaultValue = "20") Integer size,
            @CurrentUser User currentUser
    ) {
        return wrap(new AiChatMessageListVo(
                aiChatService.getMessageList(sessionId, currentUser, lastId, size)
        ));
    }
}
