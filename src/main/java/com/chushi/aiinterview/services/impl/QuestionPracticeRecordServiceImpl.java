package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.dto.QuestionPracticeRecordCreateDto;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.UserRoles;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordDailyStatVo;
import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordStatVo;
import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordVo;
import com.chushi.aiinterview.entities.QuestionPracticeRecord;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.QuestionMapper;
import com.chushi.aiinterview.mappers.QuestionPracticeRecordMapper;
import com.chushi.aiinterview.services.QuestionPracticeRecordService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class QuestionPracticeRecordServiceImpl implements QuestionPracticeRecordService {
    @Resource
    private QuestionPracticeRecordMapper questionPracticeRecordMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private IdGenerator<Long> idGenerator;

    @Override
    @Transactional
    public QuestionPracticeRecordVo createPracticeRecord(User currentUser, QuestionPracticeRecordCreateDto record) {
        // 先确认题目存在，并复用会员题可见性规则
        var question = questionMapper.findById(record.getQuestionId()).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question not found")
        );
        if (question.getIsMemberOnly() != null && question.getIsMemberOnly() == 1) {
            var userRoles = new UserRoles(currentUser.getRoles());
            if (!userRoles.hasAny(UserRole.ADMIN, UserRole.SUPER_ADMIN)) {
                throw new BusinessException(HttpServletResponse.SC_FORBIDDEN, "Member only question");
            }
        }

        var now = TimeUtils.currentLocalDateTime();
        // TODO: 后续接入 AI 分析时，再把刷题记录扩成更完整的学习行为沉淀和判题结果体系
        var practiceRecord = QuestionPracticeRecord.builder()
                .id(idGenerator.nextId())
                .userId(currentUser.getId())
                .questionId(question.getId())
                .isCorrect(record.getIsCorrect())
                .durationSeconds(record.getDurationSeconds())
                .practiceDate(now.toLocalDate())
                .createTime(now)
                .updateTime(now)
                .build();

        try {
            var affectedRows = questionPracticeRecordMapper.insert(practiceRecord);
            if (affectedRows != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Create practice record failed");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("CreatePracticeRecordException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Create practice record failed");
        }

        return QuestionPracticeRecordVo.builder()
                .id(practiceRecord.getId())
                .questionId(question.getId())
                .questionTitle(question.getTitle())
                .questionDifficulty(question.getDifficulty())
                .isCorrect(practiceRecord.getIsCorrect())
                .durationSeconds(practiceRecord.getDurationSeconds())
                .practiceDate(practiceRecord.getPracticeDate())
                .createTime(practiceRecord.getCreateTime())
                .build();
    }

    @Override
    public List<QuestionPracticeRecordVo> getPracticeRecordList(User currentUser,
                                                                Long cursor,
                                                                Integer limit) {
        return questionPracticeRecordMapper.findPracticeRecordList(currentUser.getId(), cursor, limit);
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
            todayPracticeCount = getTodayPracticeCount(dailyRecords, today);
            todayCorrectCount = countTodayCorrectRecords(currentUser.getId(), today);
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

    private Integer getTodayPracticeCount(List<QuestionPracticeRecordDailyStatVo> dailyRecords, LocalDate today) {
        var todayString = today.toString();
        for (var dailyRecord : dailyRecords) {
            if (todayString.equals(dailyRecord.getDate())) {
                return dailyRecord.getCount();
            }
        }
        return 0;
    }

    private Integer countTodayCorrectRecords(Long userId, LocalDate today) {
        // 今天答对次数单独查库，避免从全年聚合结果里再推导正确数
        return questionPracticeRecordMapper.countCorrectPracticeRecordsByDate(userId, today);
    }
}
