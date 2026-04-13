package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.dto.QuestionCreateDto;
import com.chushi.aiinterview.commons.dto.QuestionUpdateDto;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.vo.QuestionListVo;
import com.chushi.aiinterview.commons.vo.QuestionVo;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.entities.User;
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
public class QuestionController extends BaseController {
    @Resource
    private QuestionService questionService;

    @PostMapping("/api/question")
    @Operation(summary = "创建题目")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<QuestionVo> createQuestion(
            @Valid @RequestBody QuestionCreateDto questionCreateDto,
            @CurrentUser User currentUser
    ) {
        // 题目详情权限由 Service 按角色统一处理
        var question = questionService.createQuestion(currentUser, questionCreateDto);
        return wrap(QuestionVo.builder()
                .id(question.getId())
                .title(question.getTitle())
                .content(question.getContent())
                .tags(question.getTags())
                .answer(question.getAnswer())
                .difficulty(question.getDifficulty())
                .isMemberOnly(question.getIsMemberOnly())
                .editTime(question.getEditTime())
                .createTime(question.getCreateTime())
                .updateTime(question.getUpdateTime())
                .build());
    }

    @GetMapping("/api/question/{questionId}")
    @Operation(summary = "获取题目详情")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<Object> getQuestion(@PathVariable Long questionId, @CurrentUser User currentUser)
    {
        return wrap(questionService.getQuestionById(questionId, currentUser));
    }

    @GetMapping("/api/questions")
    @Operation(summary = "获取题目列表")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<QuestionListVo> getQuestionList(
            @Parameter(description = "分页游标，表示上一个题目的 ID")
            @PositiveOrZero(message = "Last ID must be >= 0")
            @RequestParam(name = "last_id", required = false) Long lastId,
            @Parameter(description = "每页数量，范围 1-50")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 50, message = "Size must be <= 50")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "创建者 ID")
            @RequestParam(name = "user_id", required = false) Long userId
    ) {
        return wrap(new QuestionListVo(questionService.getQuestionList(lastId, size, userId)));
    }

    @PutMapping("/api/question/{questionId}")
    @Operation(summary = "更新题目")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<Void> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionUpdateDto questionUpdateDto,
            @CurrentUser User currentUser
    ) {
        questionService.updateQuestion(questionId, currentUser.getId(), questionUpdateDto);
        return wrap();
    }

    @DeleteMapping("/api/question/{questionId}")
    @Operation(summary = "删除题目")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<Void> removeQuestion(
            @PathVariable Long questionId,
            @CurrentUser User currentUser
    ) {
        questionService.removeQuestion(questionId, currentUser.getId());
        return wrap();
    }
}
