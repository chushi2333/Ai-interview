package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.entities.QuestionSelfTest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface QuestionSelfTestMapper {
    @Insert("""
            INSERT INTO question_self_test (id, question_id, content, explanation, create_time, update_time, is_delete)
            VALUES (#{id}, #{questionId}, #{content}, #{explanation}, #{createTime}, #{updateTime}, #{isDelete})
            """)
    int insert(QuestionSelfTest selfTest);

    @Select("SELECT * FROM question_self_test WHERE id = #{id} AND is_delete = 0")
    Optional<QuestionSelfTest> findById(Long id);

    @Update("""
            UPDATE question_self_test
            SET is_delete = 1,
                update_time = #{updateTime}
            WHERE id = #{id} AND is_delete = 0
            """)
    int removeById(@Param("id") Long id,
                   @Param("updateTime") LocalDateTime updateTime);

    List<QuestionSelfTest> findByQuestionId(Long questionId);
}
