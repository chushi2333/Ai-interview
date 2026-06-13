package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.commons.vo.AiChatMessageVo;
import com.chushi.aiinterview.entities.AiChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AiChatMessageMapper {
    @Insert("""
            INSERT INTO ai_chat_message (id, session_id, user_id, question_id, role, content, model_name, status, error_message, latency_ms, create_time, update_time)
            VALUES (#{id}, #{sessionId}, #{userId}, #{questionId}, #{role}, #{content}, #{modelName}, #{status}, #{errorMessage}, #{latencyMs}, #{createTime}, #{updateTime})
            """)
    int insert(AiChatMessage message);

    List<AiChatMessageVo> findMessageListBySessionId(@Param("sessionId") Long sessionId,
                                                     @Param("userId") Long userId,
                                                     @Param("cursor") Long cursor,
                                                     @Param("limit") Integer limit);

    List<AiChatMessageVo> findRecentMessagesBySessionId(@Param("sessionId") Long sessionId,
                                                        @Param("userId") Long userId,
                                                        @Param("limit") Integer limit);
}
