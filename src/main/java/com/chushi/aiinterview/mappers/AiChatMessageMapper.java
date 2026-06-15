package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.commons.vo.AiChatMessageVo;
import com.chushi.aiinterview.entities.AiChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    @Select("""
            SELECT COUNT(*)
            FROM ai_chat_message
            WHERE session_id = #{sessionId}
              AND user_id = #{userId}
              AND status = 'success'
              AND TRIM(content) != ''
            """)
    int countSuccessMessagesBySessionId(@Param("sessionId") Long sessionId,
                                        @Param("userId") Long userId);


    @Select("""
            SELECT COUNT(*)
            FROM ai_chat_message
            WHERE session_id = #{sessionId}
              AND user_id = #{userId}
              AND status = 'success'
              AND TRIM(content) != ''
              AND id < #{beforeMessageId}
              AND (#{summaryMessageId} IS NULL OR id > #{summaryMessageId})
            """)
    int countSummaryMessagesBySessionId(@Param("sessionId") Long sessionId,
                                        @Param("userId") Long userId,
                                        @Param("summaryMessageId") Long summaryMessageId,
                                        @Param("beforeMessageId") Long beforeMessageId);

    List<AiChatMessageVo> findSummaryMessagesBySessionId(@Param("sessionId") Long sessionId,
                                                         @Param("userId") Long userId,
                                                         @Param("summaryMessageId") Long summaryMessageId,
                                                         @Param("beforeMessageId") Long beforeMessageId,
                                                         @Param("limit") Integer limit);

    List<AiChatMessageVo> findRecentMessagesBySessionId(@Param("sessionId") Long sessionId,
                                                        @Param("userId") Long userId,
                                                        @Param("limit") Integer limit);
}
