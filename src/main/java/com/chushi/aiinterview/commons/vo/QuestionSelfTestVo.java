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
public class QuestionSelfTestVo {
    private Long id;

    private Long questionId;

    private String content;

    private List<QuestionSelfTestOptionVo> options;
}
