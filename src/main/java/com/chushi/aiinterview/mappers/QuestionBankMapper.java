package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.entities.QuestionBank;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
// 题库数据访问接口
public interface QuestionBankMapper {
    @Insert("""
            INSERT INTO question_bank (id, title, description, picture, user_id, edit_time, create_time, update_time, is_delete)
            VALUES (#{id}, #{title}, #{description}, #{picture}, #{userId}, #{editTime}, #{createTime}, #{updateTime}, #{isDelete})
            """)
    int insert(QuestionBank questionBank);

    @Select("SELECT * FROM question_bank WHERE id = #{id} AND is_delete = 0")
    Optional<QuestionBank> findById(Long id);

    @Update("""
            UPDATE question_bank
            SET title = #{title},
                description = #{description},
                picture = #{picture},
                edit_time = #{editTime},
                update_time = #{updateTime}
            WHERE id = #{id} AND is_delete = 0
            """)
    int updateQuestionBank(QuestionBank questionBank);

    @Update("""
            UPDATE question_bank
            SET is_delete = 1,
                update_time = #{updateTime}
            WHERE id = #{id} AND is_delete = 0
            """)
    int removeQuestionBank(@Param("id") Long id,
                           @Param("updateTime") LocalDateTime updateTime);

    List<QuestionBank> findQuestionBankList(@Param("cursor") Long cursor,
                                            @Param("limit") Integer limit,
                                            @Param("userId") Long userId);
}
