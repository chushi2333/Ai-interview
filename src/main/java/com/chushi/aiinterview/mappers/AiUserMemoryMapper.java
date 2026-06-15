package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.entities.AiUserMemory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface AiUserMemoryMapper {
    @Select("""
            SELECT id, user_id AS userId, memory_summary AS memorySummary, source_session_count AS sourceSessionCount,
                   last_source_session_id AS lastSourceSessionId, create_time AS createTime, update_time AS updateTime
            FROM ai_user_memory
            WHERE user_id = #{userId}
            """)
    Optional<AiUserMemory> findByUserId(Long userId);

    @Insert("""
            INSERT INTO ai_user_memory (id, user_id, memory_summary, source_session_count, last_source_session_id, create_time, update_time)
            VALUES (#{id}, #{userId}, #{memorySummary}, #{sourceSessionCount}, #{lastSourceSessionId}, #{createTime}, #{updateTime})
            """)
    int insert(AiUserMemory memory);

    @Update("""
            UPDATE ai_user_memory
            SET memory_summary = #{memorySummary}, source_session_count = #{sourceSessionCount},
                last_source_session_id = #{lastSourceSessionId}, update_time = #{updateTime}
            WHERE user_id = #{userId}
            """)
    int updateByUserId(@Param("userId") Long userId,
                       @Param("memorySummary") String memorySummary,
                       @Param("sourceSessionCount") Integer sourceSessionCount,
                       @Param("lastSourceSessionId") Long lastSourceSessionId,
                       @Param("updateTime") LocalDateTime updateTime);
}
