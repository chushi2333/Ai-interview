package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordStatVo;
import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordVo;
import com.chushi.aiinterview.entities.User;

import java.time.LocalDate;
import java.util.List;

public interface QuestionPracticeRecordService {
    List<QuestionPracticeRecordVo> getPracticeRecordList(User currentUser,
                                                         Long cursor,
                                                         Integer limit);

    List<QuestionPracticeRecordVo> getPracticeRecordListByDate(User currentUser, LocalDate date);

    QuestionPracticeRecordStatVo getPracticeRecordStat(User currentUser, Integer year);
}
