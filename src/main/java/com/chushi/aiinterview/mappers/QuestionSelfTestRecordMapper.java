package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.commons.vo.QuestionWrongBookVo;
import com.chushi.aiinterview.entities.QuestionSelfTestRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionSelfTestRecordMapper {
    @Insert("""
            INSERT INTO question_self_test_record (id, self_test_id, user_id, selected_answers, is_correct, duration_seconds, create_time, update_time)
            VALUES (#{id}, #{selfTestId}, #{userId}, #{selectedAnswers}, #{isCorrect}, #{durationSeconds}, #{createTime}, #{updateTime})
            """)
    int insert(QuestionSelfTestRecord record);

    List<QuestionWrongBookVo> findWrongBookList(@Param("userId") Long userId,
                                                @Param("cursor") Long cursor,
                                                @Param("limit") Integer limit,
                                                @Param("canViewMemberOnly") Integer canViewMemberOnly);
}
