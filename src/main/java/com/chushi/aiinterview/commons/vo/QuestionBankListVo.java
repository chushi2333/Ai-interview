package com.chushi.aiinterview.commons.vo;

import com.chushi.aiinterview.entities.QuestionBank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionBankListVo {
    private List<QuestionBank> questionBankList;
}
