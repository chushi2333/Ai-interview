package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSelfTestManageVo {
    private Long id;

    private Long questionId;

    private String content;

    private String explanation;

    private List<QuestionSelfTestManageOptionVo> options;
}
