package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.entities.Question;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

@Mapper
// 题目数据访问接口
public interface QuestionMapper {
    @Insert("""
            INSERT INTO question (id, title, content, tags, answer, difficulty, is_member_only, user_id, edit_time, create_time, update_time, is_delete)
            VALUES (#{id}, #{title}, #{content}, #{tags}, #{answer}, #{difficulty}, #{isMemberOnly}, #{userId}, #{editTime}, #{createTime}, #{updateTime}, #{isDelete})
            """)
    int insert(Question question);

    @Select("SELECT * FROM question WHERE id = #{id} AND is_delete = 0")
    Optional<Question> findById(Long id);

    @Update("""
            UPDATE question
            SET title = #{title},
                content = #{content},
                tags = #{tags},
                answer = #{answer},
                difficulty = #{difficulty},
                is_member_only = #{isMemberOnly},
                edit_time = #{editTime},
                update_time = #{updateTime}
            WHERE id = #{id} AND is_delete = 0
            """)
    int updateQuestion(Question question);

    @Update("""
            UPDATE question
            SET is_delete = 1,
                update_time = #{updateTime}
            WHERE id = #{id} AND is_delete = 0
            """)
    int removeQuestion(@Param("id") Long id,
                       @Param("updateTime") java.time.LocalDateTime updateTime);

    List<Question> findQuestionList(@Param("cursor") Long cursor,
                                    @Param("limit") Integer limit,
                                    @Param("userId") Long userId);

    List<Question> findQuestionListByQuestionBankId(@Param("questionBankId") Long questionBankId,
                                                    @Param("cursor") Long cursor,
                                                    @Param("limit") Integer limit);
}
