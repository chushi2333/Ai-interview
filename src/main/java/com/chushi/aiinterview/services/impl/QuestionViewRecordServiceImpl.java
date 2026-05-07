package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.commons.vo.QuestionViewRecordDailyStatVo;
import com.chushi.aiinterview.commons.vo.QuestionViewRecordStatVo;
import com.chushi.aiinterview.commons.vo.QuestionViewRecordVo;
import com.chushi.aiinterview.entities.Question;
import com.chushi.aiinterview.entities.QuestionViewRecord;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.mappers.QuestionViewRecordMapper;
import com.chushi.aiinterview.services.QuestionViewRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class QuestionViewRecordServiceImpl implements QuestionViewRecordService {
    @Resource
    private QuestionViewRecordMapper questionViewRecordMapper;

    @Resource
    private IdGenerator<Long> idGenerator;

    @Override
    public void recordQuestionView(User currentUser, Question question) {
        var now = TimeUtils.currentLocalDateTime();
        var viewRecord = QuestionViewRecord.builder()
                .id(idGenerator.nextId())
                .userId(currentUser.getId())
                .questionId(question.getId())
                .viewDate(now.toLocalDate())
                .createTime(now)
                .updateTime(now)
                .build();

        try {
            // 同一天反复打开同一道题只记一条，避免刷新页面把学习热度刷爆
            questionViewRecordMapper.insertIgnore(viewRecord);
        } catch (Exception e) {
            // 看题是主流程，留痕失败只记日志，不反向影响题目详情读取
            log.error("RecordQuestionViewException: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<QuestionViewRecordVo> getViewRecordList(User currentUser, Long cursor, Integer limit) {
        return questionViewRecordMapper.findViewRecordList(currentUser.getId(), cursor, limit);
    }

    @Override
    public QuestionViewRecordStatVo getViewRecordStat(User currentUser, Integer year) {
        var startDate = LocalDate.of(year, 1, 1);
        var endDate = LocalDate.of(year, 12, 31);
        var rawDailyRecords = questionViewRecordMapper.countDailyViewRecords(currentUser.getId(), startDate, endDate);
        // 补齐全年空日期后，前端可以直接拿来看题热力图
        var dailyRecords = fillMissingDailyRecords(year, rawDailyRecords);
        var todayViewCount = 0;
        var today = TimeUtils.currentLocalDateTime().toLocalDate();
        if (today.getYear() == year) {
            todayViewCount = getTodayViewCount(dailyRecords, today);
        }
        return new QuestionViewRecordStatVo(year, todayViewCount, dailyRecords);
    }

    private List<QuestionViewRecordDailyStatVo> fillMissingDailyRecords(Integer year,
                                                                        List<QuestionViewRecordDailyStatVo> rawDailyRecords) {
        var dailyRecordMap = new HashMap<String, Integer>();
        for (var dailyRecord : rawDailyRecords) {
            dailyRecordMap.put(dailyRecord.getDate(), dailyRecord.getCount());
        }

        var date = LocalDate.of(year, 1, 1);
        var endDate = LocalDate.of(year, 12, 31);
        var filledDailyRecords = new java.util.ArrayList<QuestionViewRecordDailyStatVo>();
        while (!date.isAfter(endDate)) {
            var dateString = date.toString();
            filledDailyRecords.add(new QuestionViewRecordDailyStatVo(
                    dateString,
                    dailyRecordMap.getOrDefault(dateString, 0)
            ));
            date = date.plusDays(1);
        }
        return filledDailyRecords;
    }

    private Integer getTodayViewCount(List<QuestionViewRecordDailyStatVo> dailyRecords, LocalDate today) {
        var todayString = today.toString();
        for (var dailyRecord : dailyRecords) {
            if (todayString.equals(dailyRecord.getDate())) {
                return dailyRecord.getCount();
            }
        }
        return 0;
    }
}
