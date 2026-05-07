package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.commons.vo.UserSignInRecordsVo;
import com.chushi.aiinterview.commons.vo.UserSignInStatVo;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.UserSignInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;

@RestController
public class UserSignInController extends BaseController {
    @Resource
    private UserSignInService userSignInService;

    @PostMapping("/api/user/sign-in")
    @Operation(summary = "用户签到")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<UserSignInStatVo> signIn(@CurrentUser User currentUser) {
        return wrap(userSignInService.signIn(currentUser));
    }

    @GetMapping("/api/user/sign-in/stat")
    @Operation(summary = "获取签到统计")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<UserSignInStatVo> getSignInStat(@CurrentUser User currentUser) {
        return wrap(userSignInService.getSignInStat(currentUser));
    }

    @GetMapping("/api/user/sign-in/records")
    @Operation(summary = "获取某年签到记录")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<UserSignInRecordsVo> getSignInRecords(
            @CurrentUser User currentUser,
            @Parameter(description = "年份")
            @Min(value = 2000, message = "Year must be >= 2000")
            @Max(value = 2100, message = "Year must be <= 2100")
            @RequestParam(required = false) Integer year
    ) {
        return wrap(userSignInService.getSignInRecords(currentUser, year == null ? Year.now().getValue() : year));
    }
}
