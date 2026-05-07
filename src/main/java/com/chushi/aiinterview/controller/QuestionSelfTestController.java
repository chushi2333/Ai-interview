package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.dto.QuestionSelfTestCreateDto;
import com.chushi.aiinterview.commons.dto.QuestionSelfTestSubmitDto;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestListVo;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestManageVo;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestSubmitResultVo;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.QuestionSelfTestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class QuestionSelfTestController extends BaseController {
    @Resource
    private QuestionSelfTestService questionSelfTestService;

    @PostMapping("/api/question/{questionId}/self-test")
    @Operation(summary = "创建题目自测题")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<QuestionSelfTestManageVo> createSelfTest(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionSelfTestCreateDto questionSelfTestCreateDto
    ) {
        // 自测题由管理员维护，和题库内容一样属于官方内容
        return wrap(questionSelfTestService.createSelfTest(questionId, questionSelfTestCreateDto));
    }

    @GetMapping("/api/question/{questionId}/self-tests")
    @Operation(summary = "获取题目下的自测题列表")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<QuestionSelfTestListVo> getSelfTestsByQuestionId(
            @PathVariable Long questionId,
            @CurrentUser User currentUser
    ) {
        // 用户侧只拿到做题信息，不暴露正确答案和正确选项标记
        return wrap(questionSelfTestService.getSelfTestsByQuestionId(questionId, currentUser));
    }

    @PostMapping("/api/self-test/{selfTestId}/submit")
    @Operation(summary = "提交自测题答案")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<QuestionSelfTestSubmitResultVo> submitSelfTest(
            @PathVariable Long selfTestId,
            @Valid @RequestBody QuestionSelfTestSubmitDto questionSelfTestSubmitDto,
            @CurrentUser User currentUser
    ) {
        // 提交后直接返回判题结果和解析，前端不用再额外查一次
        return wrap(questionSelfTestService.submitSelfTest(selfTestId, currentUser, questionSelfTestSubmitDto));
    }

    @DeleteMapping("/api/self-test/{selfTestId}")
    @Operation(summary = "删除自测题")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<Void> removeSelfTest(@PathVariable Long selfTestId) {
        questionSelfTestService.removeSelfTest(selfTestId);
        return wrap();
    }
}
