package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSelfTestManageOptionVo {
    private Long id;

    private String optionKey;

    private String content;

    private Integer isCorrect;

    private Integer sortOrder;
}
