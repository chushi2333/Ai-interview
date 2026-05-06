package com.chushi.aiinterview.commons.vo;

import com.chushi.aiinterview.entities.QuestionES;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSearchVo {
    private List<QuestionES> questionList;
}
