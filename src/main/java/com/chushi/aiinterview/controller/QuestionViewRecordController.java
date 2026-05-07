package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.vo.QuestionViewRecordListVo;
import com.chushi.aiinterview.commons.vo.QuestionViewRecordStatVo;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.QuestionViewRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuestionViewRecordController extends BaseController {
    @Resource
    private QuestionViewRecordService questionViewRecordService;

    @GetMapping("/api/question/view-records")
    @Operation(summary = "获取看题记录列表")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<QuestionViewRecordListVo> getViewRecordList(
            @Parameter(description = "分页游标，表示上一条看题记录 ID")
            @PositiveOrZero(message = "Last ID must be >= 0")
            @RequestParam(name = "last_id", required = false) Long lastId,
            @Parameter(description = "每页数量，范围 1-50")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 50, message = "Size must be <= 50")
            @RequestParam(defaultValue = "10") Integer size,
            @CurrentUser User currentUser
    ) {
        return wrap(new QuestionViewRecordListVo(
                questionViewRecordService.getViewRecordList(currentUser, lastId, size)
        ));
    }

    @GetMapping("/api/question/view-record/stat")
    @Operation(summary = "获取看题记录统计")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<QuestionViewRecordStatVo> getViewRecordStat(
            @Parameter(description = "统计年份")
            @Min(value = 2000, message = "Year must be >= 2000")
            @Max(value = 2100, message = "Year must be <= 2100")
            @RequestParam(required = false) Integer year,
            @CurrentUser User currentUser
    ) {
        var targetYear = year == null ? TimeUtils.currentLocalDateTime().toLocalDate().getYear() : year;
        return wrap(questionViewRecordService.getViewRecordStat(currentUser, targetYear));
    }
}
