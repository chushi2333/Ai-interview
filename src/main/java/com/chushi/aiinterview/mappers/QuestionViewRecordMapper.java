package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.commons.vo.QuestionViewRecordDailyStatVo;
import com.chushi.aiinterview.commons.vo.QuestionViewRecordVo;
import com.chushi.aiinterview.entities.QuestionViewRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface QuestionViewRecordMapper {
    @Insert("""
            INSERT IGNORE INTO question_view_record (id, user_id, question_id, view_date, create_time, update_time)
            VALUES (#{id}, #{userId}, #{questionId}, #{viewDate}, #{createTime}, #{updateTime})
            """)
    int insertIgnore(QuestionViewRecord record);

    List<QuestionViewRecordVo> findViewRecordList(@Param("userId") Long userId,
                                                  @Param("cursor") Long cursor,
                                                  @Param("limit") Integer limit);

    List<QuestionViewRecordDailyStatVo> countDailyViewRecords(@Param("userId") Long userId,
                                                              @Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate);
}
