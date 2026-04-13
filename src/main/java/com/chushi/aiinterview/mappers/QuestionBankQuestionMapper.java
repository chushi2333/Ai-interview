package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.entities.QuestionBankQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
// 题库题目关系数据访问接口
public interface QuestionBankQuestionMapper {
    @Insert("""
            INSERT INTO question_bank_question (id, question_bank_id, question_id, user_id, create_time, update_time)
            VALUES (#{id}, #{questionBankId}, #{questionId}, #{userId}, #{createTime}, #{updateTime})
            """)
    int insert(QuestionBankQuestion questionBankQuestion);

    void insertBatch(@Param("relations") List<QuestionBankQuestion> relations);

    @Delete("""
            DELETE FROM question_bank_question
            WHERE question_bank_id = #{questionBankId} AND question_id = #{questionId}
            """)
    int removeByQuestionBankIdAndQuestionId(@Param("questionBankId") Long questionBankId,
                                            @Param("questionId") Long questionId);

    @Delete("""
            DELETE FROM question_bank_question
            WHERE question_bank_id = #{questionBankId}
            """)
    int removeByQuestionBankId(@Param("questionBankId") Long questionBankId);

    @Delete("""
            DELETE FROM question_bank_question
            WHERE question_id = #{questionId}
            """)
    int removeByQuestionId(@Param("questionId") Long questionId);

    @Select("""
            SELECT COUNT(*)
            FROM question_bank_question
            WHERE question_bank_id = #{questionBankId} AND question_id = #{questionId}
            """)
    long countByQuestionBankIdAndQuestionId(@Param("questionBankId") Long questionBankId,
                                            @Param("questionId") Long questionId);
}
