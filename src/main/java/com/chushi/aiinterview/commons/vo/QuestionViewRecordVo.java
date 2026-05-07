package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionViewRecordVo {
    private Long id;

    private Long questionId;

    private String questionTitle;

    private Integer questionDifficulty;

    private LocalDate viewDate;

    private LocalDateTime createTime;
}
