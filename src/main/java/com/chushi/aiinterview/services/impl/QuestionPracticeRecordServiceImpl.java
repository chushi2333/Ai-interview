package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordDailyStatVo;
import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordStatVo;
import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordVo;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.mappers.QuestionPracticeRecordMapper;
import com.chushi.aiinterview.services.QuestionPracticeRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@Service
public class QuestionPracticeRecordServiceImpl implements QuestionPracticeRecordService {
    @Resource
    private QuestionPracticeRecordMapper questionPracticeRecordMapper;

    @Override
    public List<QuestionPracticeRecordVo> getPracticeRecordList(User currentUser,
                                                                Long cursor,
                                                                Integer limit) {
        return questionPracticeRecordMapper.findPracticeRecordList(currentUser.getId(), cursor, limit);
    }

    @Override
    public List<QuestionPracticeRecordVo> getPracticeRecordListByDate(User currentUser, LocalDate date) {
        return questionPracticeRecordMapper.findPracticeRecordListByDate(currentUser.getId(), date);
    }

    @Override
    public QuestionPracticeRecordStatVo getPracticeRecordStat(User currentUser, Integer year) {
        var startDate = LocalDate.of(year, 1, 1);
        var endDate = LocalDate.of(year, 12, 31);
        var rawDailyRecords = questionPracticeRecordMapper.countDailyPracticeRecords(currentUser.getId(), startDate, endDate);
        // 先补齐全年每天的数据，前端画热力图时不用自己补 0
        var dailyRecords = fillMissingDailyRecords(year, rawDailyRecords);
        var todayPracticeCount = 0;
        var todayCorrectCount = 0;
        var today = TimeUtils.currentLocalDateTime().toLocalDate();

        if (today.getYear() == year) {
            var todayString = today.toString();
            for (var dailyRecord : dailyRecords) {
                if (todayString.equals(dailyRecord.getDate())) {
                    todayPracticeCount = dailyRecord.getCount();
                    break;
                }
            }
            // 今天答对次数单独查库，避免从全年聚合结果里再推导正确数
            todayCorrectCount = questionPracticeRecordMapper.countCorrectPracticeRecordsByDate(currentUser.getId(), today);
        }

        return new QuestionPracticeRecordStatVo(year, todayPracticeCount, todayCorrectCount, dailyRecords);
    }

    private List<QuestionPracticeRecordDailyStatVo> fillMissingDailyRecords(Integer year,
                                                                            List<QuestionPracticeRecordDailyStatVo> rawDailyRecords) {
        var dailyRecordMap = new HashMap<String, Integer>();
        for (var dailyRecord : rawDailyRecords) {
            dailyRecordMap.put(dailyRecord.getDate(), dailyRecord.getCount());
        }

        var date = LocalDate.of(year, 1, 1);
        var endDate = LocalDate.of(year, 12, 31);
        var filledDailyRecords = new java.util.ArrayList<QuestionPracticeRecordDailyStatVo>();
        // 补齐空日期后，前端可以直接画全年热力图，不用自己再做日期对齐
        while (!date.isAfter(endDate)) {
            var dateString = date.toString();
            filledDailyRecords.add(new QuestionPracticeRecordDailyStatVo(
                    dateString,
                    dailyRecordMap.getOrDefault(dateString, 0)
            ));
            date = date.plusDays(1);
        }
        return filledDailyRecords;
    }
}
