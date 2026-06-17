package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.annotations.NoAuth;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.dto.QuestionBankQuestionAddDto;
import com.chushi.aiinterview.commons.dto.QuestionBankQuestionBatchAddDto;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.vo.QuestionBankQuestionBatchAddVo;
import com.chushi.aiinterview.commons.vo.QuestionListVo;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.QuestionBankQuestionService;
import com.chushi.aiinterview.services.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.*;

@RestController
// 题库题目关系接口
public class QuestionBankQuestionController extends BaseController {
    @Resource
    private QuestionBankQuestionService questionBankQuestionService;

    @Resource
    private QuestionService questionService;

    @PostMapping("/api/question-bank/question")
    @Operation(summary = "向题库添加题目")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<Void> addQuestionToBank(
            @Valid @RequestBody QuestionBankQuestionAddDto dto,
            @CurrentUser User currentUser
    ) {
        questionBankQuestionService.addQuestionToBank(dto.getQuestionBankId(), dto.getQuestionId(), currentUser.getId());
        return wrap();
    }

    @PostMapping("/api/question-bank/questions")
    @Operation(summary = "批量向题库添加题目")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<QuestionBankQuestionBatchAddVo> addQuestionsToBank(
            @Valid @RequestBody QuestionBankQuestionBatchAddDto dto,
            @CurrentUser User currentUser
    ) {
        // 返回本次请求数量和实际新增数量，便于前端做提示
        var addedCount = questionBankQuestionService.addQuestionsToBank(dto.getQuestionBankId(), dto.getQuestionIds(), currentUser.getId());
        return wrap(new QuestionBankQuestionBatchAddVo(dto.getQuestionIds().size(), addedCount));
    }

    @DeleteMapping("/api/question-bank/{questionBankId}/question/{questionId}")
    @Operation(summary = "从题库移除题目")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<Void> removeQuestionFromBank(
            @PathVariable Long questionBankId,
            @PathVariable Long questionId,
            @CurrentUser User currentUser
    ) {
        questionBankQuestionService.removeQuestionFromBank(questionBankId, questionId, currentUser.getId());
        return wrap();
    }

    @GetMapping("/api/question-bank/{questionBankId}/questions")
    @Operation(summary = "获取题库下的题目列表")
    @NoAuth
    public Response<QuestionListVo> getQuestionListByQuestionBankId(
            @PathVariable Long questionBankId,
            @Parameter(description = "分页游标，表示上一个题目的 ID")
            @PositiveOrZero(message = "Last ID must be >= 0")
            @RequestParam(name = "last_id", required = false) Long lastId,
            @Parameter(description = "每页数量，范围 1-50")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 50, message = "Size must be <= 50")
            @RequestParam(defaultValue = "10") Integer size
    ) {
        // 题库公开可见，但题目详情权限仍由题目业务处理
        return wrap(new QuestionListVo(questionService.getQuestionListByQuestionBankId(questionBankId, lastId, size)));
    }
}
