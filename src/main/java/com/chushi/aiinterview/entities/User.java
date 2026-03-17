package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

// 用户实体类，表示系统中的用户
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    public static final User CacheNull = new User(-1L, "", "", "", "", null, 0L);

    // 用户的唯一标识符
    private Long id;

    // 用户昵称
    private String nickname;

    // 用户头像
    private String avatar;

    // 用户账户的加密密码
    @Nullable
    private String password;

    //    // 用户的电子邮件地址
    //    private String email;
    //
    // 用户的电话号码
    private String phone;

    // 用户账户创建的时间戳
    private LocalDateTime joinTime;

    // 用户角色
    private Long roles;
}

