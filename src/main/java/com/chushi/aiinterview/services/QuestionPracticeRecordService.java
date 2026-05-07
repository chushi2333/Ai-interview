package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordStatVo;
import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordVo;
import com.chushi.aiinterview.entities.User;

import java.util.List;

public interface QuestionPracticeRecordService {
    List<QuestionPracticeRecordVo> getPracticeRecordList(User currentUser,
                                                         Long cursor,
                                                         Integer limit);

    QuestionPracticeRecordStatVo getPracticeRecordStat(User currentUser, Integer year);
}
