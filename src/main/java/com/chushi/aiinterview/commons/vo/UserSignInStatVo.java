package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSignInStatVo {
    private Boolean signedToday;

    private Integer currentStreak;

    private Integer currentMonthSignDays;

    private Integer totalSignDays;
}
