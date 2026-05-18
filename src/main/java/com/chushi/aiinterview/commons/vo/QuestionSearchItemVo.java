package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSearchItemVo {
    private Long id;

    private String title;

    private String content;

    private String answer;

    private List<String> tags;

    private Integer difficulty;

    private Integer isMemberOnly;

    private Long userId;

    private Long questionBankId;

    private String questionBankTitle;

    private Instant createdAt;
}
