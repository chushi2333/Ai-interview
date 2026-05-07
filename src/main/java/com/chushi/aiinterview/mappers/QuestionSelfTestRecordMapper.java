package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.entities.QuestionSelfTestRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionSelfTestRecordMapper {
    @Insert("""
            INSERT INTO question_self_test_record (id, self_test_id, user_id, selected_answers, is_correct, duration_seconds, create_time, update_time)
            VALUES (#{id}, #{selfTestId}, #{userId}, #{selectedAnswers}, #{isCorrect}, #{durationSeconds}, #{createTime}, #{updateTime})
            """)
    int insert(QuestionSelfTestRecord record);
}
