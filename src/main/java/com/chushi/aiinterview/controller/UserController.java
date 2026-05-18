package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.commons.dto.UserPasswordUpdateDto;
import com.chushi.aiinterview.commons.dto.UserProfileUpdateDto;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.commons.vo.UserProfileVo;
import com.chushi.aiinterview.commons.vo.UserStudyRecordsVo;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.QuestionPracticeRecordService;
import com.chushi.aiinterview.services.QuestionViewRecordService;
import com.chushi.aiinterview.services.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class UserController extends BaseController {
    @Resource
    private UserService userService;

    @Resource
    private QuestionViewRecordService questionViewRecordService;

    @Resource
    private QuestionPracticeRecordService questionPracticeRecordService;

    @GetMapping("/api/user/me")
    public Response<UserProfileVo> getCurrentUser(@CurrentUser User currentUser) {
        return wrap(buildUserProfileVo(currentUser));
    }

    @PutMapping("/api/user/profile")
    public Response<UserProfileVo> updateUserProfile(@Valid @RequestBody UserProfileUpdateDto updateDto,
                                                     @CurrentUser User currentUser) {
        var user = userService.updateNickname(currentUser.getId(), updateDto.getNickname());
        return wrap(buildUserProfileVo(user));
    }

    @PutMapping("/api/user/password")
    public Response<Void> updateUserPassword(@Valid @RequestBody UserPasswordUpdateDto updateDto,
                                             @CurrentUser User currentUser) {
        userService.updatePassword(currentUser.getId(), updateDto.getOldPassword(), updateDto.getNewPassword());
        return wrap();
    }

    @GetMapping("/api/user/study-records")
    public Response<UserStudyRecordsVo> getUserStudyRecords(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam LocalDate date,
            @CurrentUser User currentUser
    ) {
        return wrap(UserStudyRecordsVo.builder()
                .date(date)
                .viewRecords(questionViewRecordService.getViewRecordListByDate(currentUser, date))
                .practiceRecords(questionPracticeRecordService.getPracticeRecordListByDate(currentUser, date))
                .build());
    }

    private UserProfileVo buildUserProfileVo(User user) {
        return UserProfileVo.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .roles(user.getRoles())
                .joinTime(user.getJoinTime())
                .build();
    }
}
