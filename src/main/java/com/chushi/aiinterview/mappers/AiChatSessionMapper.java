package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.commons.vo.AiChatSessionVo;
import com.chushi.aiinterview.entities.AiChatSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface AiChatSessionMapper {
    @Insert("""
            INSERT INTO ai_chat_session (id, user_id, question_id, title, status, create_time, update_time)
            VALUES (#{id}, #{userId}, #{questionId}, #{title}, #{status}, #{createTime}, #{updateTime})
            """)
    int insert(AiChatSession session);

    @Select("""
            SELECT id, user_id AS userId, question_id AS questionId, title, status, is_delete AS isDelete, memory_summary AS memorySummary, summary_message_id AS summaryMessageId, create_time AS createTime, update_time AS updateTime
            FROM ai_chat_session
            WHERE id = #{id} AND is_delete = 0
            """)
    Optional<AiChatSession> findById(Long id);

    @Update("""
            UPDATE ai_chat_session
            SET update_time = #{updateTime}
            WHERE id = #{id} AND is_delete = 0
            """)
    int updateTime(@Param("id") Long id, @Param("updateTime") LocalDateTime updateTime);


    @Update("""
            UPDATE ai_chat_session
            SET title = #{title}, update_time = #{updateTime}
            WHERE id = #{id} AND user_id = #{userId} AND is_delete = 0
            """)
    int updateTitle(@Param("id") Long id,
                    @Param("userId") Long userId,
                    @Param("title") String title,
                    @Param("updateTime") LocalDateTime updateTime);

    @Update("""
            UPDATE ai_chat_session
            SET is_delete = 1, update_time = #{updateTime}
            WHERE id = #{id} AND user_id = #{userId} AND is_delete = 0
            """)
    int softDelete(@Param("id") Long id,
                   @Param("userId") Long userId,
                   @Param("updateTime") LocalDateTime updateTime);


    @Update("""
            UPDATE ai_chat_session
            SET memory_summary = #{memorySummary}, summary_message_id = #{summaryMessageId}, update_time = #{updateTime}
            WHERE id = #{id} AND user_id = #{userId} AND is_delete = 0
            """)
    int updateMemorySummary(@Param("id") Long id,
                            @Param("userId") Long userId,
                            @Param("memorySummary") String memorySummary,
                            @Param("summaryMessageId") Long summaryMessageId,
                            @Param("updateTime") LocalDateTime updateTime);

    List<AiChatSessionVo> findSessionListByQuestionId(@Param("userId") Long userId,
                                                      @Param("questionId") Long questionId,
                                                      @Param("cursor") Long cursor,
                                                      @Param("limit") Integer limit);
}
