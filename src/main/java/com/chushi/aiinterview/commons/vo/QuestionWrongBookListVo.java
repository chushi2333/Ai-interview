package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionWrongBookListVo {
    private List<QuestionWrongBookVo> questions;
}
