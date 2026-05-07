package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionPracticeRecordStatVo {
    private Integer year;

    private Integer todayPracticeCount;

    private Integer todayCorrectCount;

    private List<QuestionPracticeRecordDailyStatVo> dailyRecords;
}
