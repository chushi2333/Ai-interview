package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.vo.QuestionViewRecordStatVo;
import com.chushi.aiinterview.commons.vo.QuestionViewRecordVo;
import com.chushi.aiinterview.entities.Question;
import com.chushi.aiinterview.entities.User;

import java.util.List;
import java.time.LocalDate;

public interface QuestionViewRecordService {
    void recordQuestionView(User currentUser, Question question);

    List<QuestionViewRecordVo> getViewRecordList(User currentUser, Long cursor, Integer limit);

    List<QuestionViewRecordVo> getViewRecordListByDate(User currentUser, LocalDate date);

    QuestionViewRecordStatVo getViewRecordStat(User currentUser, Integer year);
}
