package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionViewRecordStatVo {
    private Integer year;

    private Integer todayViewCount;

    private List<QuestionViewRecordDailyStatVo> dailyRecords;
}
