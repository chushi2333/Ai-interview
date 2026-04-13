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
public class QuestionBankVo {
    private Long id;

    private String title;

    private String description;

    private String picture;

    private LocalDateTime editTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
