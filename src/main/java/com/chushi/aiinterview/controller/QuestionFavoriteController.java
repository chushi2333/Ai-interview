package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.vo.QuestionFavoriteListVo;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.QuestionFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuestionFavoriteController extends BaseController {
    @Resource
    private QuestionFavoriteService questionFavoriteService;

    @PostMapping("/api/question/{questionId}/favorite")
    @Operation(summary = "收藏题目")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<Void> favoriteQuestion(
            @PathVariable Long questionId,
            @CurrentUser User currentUser
    ) {
        questionFavoriteService.favoriteQuestion(questionId, currentUser);
        return wrap();
    }

    @DeleteMapping("/api/question/{questionId}/favorite")
    @Operation(summary = "取消收藏题目")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<Void> removeFavoriteQuestion(
            @PathVariable Long questionId,
            @CurrentUser User currentUser
    ) {
        questionFavoriteService.removeFavoriteQuestion(questionId, currentUser);
        return wrap();
    }

    @GetMapping("/api/question/favorites")
    @Operation(summary = "获取收藏题目列表")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<QuestionFavoriteListVo> getFavoriteQuestionList(
            @Parameter(description = "分页游标，表示上一条收藏记录 ID")
            @PositiveOrZero(message = "Last ID must be >= 0")
            @RequestParam(name = "last_id", required = false) Long lastId,
            @Parameter(description = "每页数量，范围 1-50")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 50, message = "Size must be <= 50")
            @RequestParam(defaultValue = "10") Integer size,
            @CurrentUser User currentUser
    ) {
        return wrap(new QuestionFavoriteListVo(
                questionFavoriteService.getFavoriteQuestionList(currentUser, lastId, size)
        ));
    }
}
