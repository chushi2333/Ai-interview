package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.dto.AiQuestionAssistRequestDto;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.vo.AiQuestionAssistVo;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.AiQuestionAssistService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
}
