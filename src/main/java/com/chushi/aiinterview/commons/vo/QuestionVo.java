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
public class QuestionVo {
    private Long id;

    private String title;

    private String content;

    private String tags;

    private String answer;

    private Integer difficulty;

    private Integer isMemberOnly;

    private Long userId;

    private Long questionBankId;

    private String questionBankTitle;

    private LocalDateTime editTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDelete;
}
