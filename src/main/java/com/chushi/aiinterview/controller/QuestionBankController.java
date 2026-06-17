package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.annotations.NoAuth;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.dto.QuestionBankCreateDto;
import com.chushi.aiinterview.commons.dto.QuestionBankUpdateDto;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.vo.QuestionBankListVo;
import com.chushi.aiinterview.commons.vo.QuestionBankVo;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.entities.QuestionBank;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.QuestionBankService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.*;

@RestController
// 题库接口
public class QuestionBankController extends BaseController {
    @Resource
    private QuestionBankService questionBankService;

    @PostMapping("/api/question-bank")
    @Operation(summary = "创建题库")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<QuestionBankVo> createQuestionBank(
            @Valid @RequestBody QuestionBankCreateDto questionBankCreateDto,
            @CurrentUser User currentUser
    ) {
        // 题库默认公开可见，这里只处理创建逻辑
        var questionBank = questionBankService.createQuestionBank(currentUser, questionBankCreateDto);
        return wrap(QuestionBankVo.builder()
                .id(questionBank.getId())
                .title(questionBank.getTitle())
                .description(questionBank.getDescription())
                .picture(questionBank.getPicture())
                .editTime(questionBank.getEditTime())
                .createTime(questionBank.getCreateTime())
                .updateTime(questionBank.getUpdateTime())
                .build());
    }

    @GetMapping("/api/question-bank/{questionBankId}")
    @Operation(summary = "获取题库详情")
    @NoAuth
    public Response<QuestionBank> getQuestionBank(
            @PathVariable Long questionBankId
    ) {
        return wrap(questionBankService.getQuestionBankById(questionBankId));
    }

    @GetMapping("/api/question-banks")
    @Operation(summary = "获取题库列表")
    @NoAuth
    public Response<QuestionBankListVo> getQuestionBankList(
            @Parameter(description = "分页游标，表示上一个题库的 ID")
            @PositiveOrZero(message = "Last ID must be >= 0")
            @RequestParam(name = "last_id", required = false) Long lastId,
            @Parameter(description = "每页数量，范围 1-50")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 50, message = "Size must be <= 50")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "创建者 ID")
            @RequestParam(name = "user_id", required = false) Long userId
    ) {
        return wrap(new QuestionBankListVo(questionBankService.getQuestionBankList(lastId, size, userId)));
    }

    @PutMapping("/api/question-bank/{questionBankId}")
    @Operation(summary = "更新题库")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<Void> updateQuestionBank(
            @PathVariable Long questionBankId,
            @Valid @RequestBody QuestionBankUpdateDto questionBankUpdateDto
    ) {
        questionBankService.updateQuestionBank(questionBankId, questionBankUpdateDto);
        return wrap();
    }

    @DeleteMapping("/api/question-bank/{questionBankId}")
    @Operation(summary = "删除题库")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<Void> removeQuestionBank(
            @PathVariable Long questionBankId
    ) {
        questionBankService.removeQuestionBank(questionBankId);
        return wrap();
    }
}
