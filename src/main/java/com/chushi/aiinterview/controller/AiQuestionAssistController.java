package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.dto.AiQuestionAssistRequestDto;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.vo.AiAssistRecordListVo;
import com.chushi.aiinterview.commons.vo.AiQuestionAssistVo;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.AiQuestionAssistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiQuestionAssistController extends BaseController {
    @Resource
    private AiQuestionAssistService aiQuestionAssistService;

    @PostMapping("/api/ai/question/{questionId}/assist")
    @Operation(summary = "AI 题解助教")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<AiQuestionAssistVo> assistQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody AiQuestionAssistRequestDto request,
            @CurrentUser User currentUser
    ) {
        return wrap(aiQuestionAssistService.assistQuestion(questionId, request, currentUser));
    }

    @GetMapping("/api/ai/question/{questionId}/assist/records")
    @Operation(summary = "获取 AI 题解助教调用记录")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<AiAssistRecordListVo> getQuestionAssistRecordList(
            @PathVariable Long questionId,
            @Parameter(description = "分页游标，表示上一条 AI 调用记录 ID")
            @PositiveOrZero(message = "Last ID must be >= 0")
            @RequestParam(name = "last_id", required = false) Long lastId,
            @Parameter(description = "每页数量，范围 1-50")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 50, message = "Size must be <= 50")
            @RequestParam(defaultValue = "10") Integer size,
            @CurrentUser User currentUser
    ) {
        return wrap(new AiAssistRecordListVo(
                aiQuestionAssistService.getQuestionAssistRecordList(questionId, currentUser, lastId, size)
        ));
    }
}
