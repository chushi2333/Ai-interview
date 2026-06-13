package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.commons.vo.AiAssistRecordVo;
import com.chushi.aiinterview.entities.AiAssistRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiAssistRecordMapper {
    @Insert("""
            INSERT INTO ai_assist_record (id, user_id, question_id, assist_type, user_input, content, model_name, status, error_message, latency_ms, create_time, update_time)
            VALUES (#{id}, #{userId}, #{questionId}, #{assistType}, #{userInput}, #{content}, #{modelName}, #{status}, #{errorMessage}, #{latencyMs}, #{createTime}, #{updateTime})
            """)
    int insert(AiAssistRecord record);

    List<AiAssistRecordVo> findRecordListByQuestionId(@Param("userId") Long userId,
                                                      @Param("questionId") Long questionId,
                                                      @Param("cursor") Long cursor,
                                                      @Param("limit") Integer limit);
}
