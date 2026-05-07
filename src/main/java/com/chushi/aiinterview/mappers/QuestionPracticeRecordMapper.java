package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordDailyStatVo;
import com.chushi.aiinterview.commons.vo.QuestionPracticeRecordVo;
import com.chushi.aiinterview.entities.QuestionPracticeRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface QuestionPracticeRecordMapper {
    @Insert("""
            INSERT INTO question_practice_record (id, user_id, question_id, is_correct, duration_seconds, practice_date, create_time, update_time)
            VALUES (#{id}, #{userId}, #{questionId}, #{isCorrect}, #{durationSeconds}, #{practiceDate}, #{createTime}, #{updateTime})
            """)
    int insert(QuestionPracticeRecord record);

    List<QuestionPracticeRecordVo> findPracticeRecordList(@Param("userId") Long userId,
                                                          @Param("cursor") Long cursor,
                                                          @Param("limit") Integer limit);

    List<QuestionPracticeRecordDailyStatVo> countDailyPracticeRecords(@Param("userId") Long userId,
                                                                      @Param("startDate") LocalDate startDate,
                                                                      @Param("endDate") LocalDate endDate);

    Integer countCorrectPracticeRecordsByDate(@Param("userId") Long userId,
                                              @Param("practiceDate") LocalDate practiceDate);
}
