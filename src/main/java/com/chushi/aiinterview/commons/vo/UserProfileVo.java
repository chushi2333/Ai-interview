package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileVo {
    private Long id;

    private String nickname;

    private String avatar;

    private String phone;

    private Long roles;

    private LocalDateTime joinTime;
}
