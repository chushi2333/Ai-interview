package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.commons.vo.QuestionFavoriteVo;
import com.chushi.aiinterview.entities.QuestionFavorite;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionFavoriteMapper {
    @Insert("""
            INSERT IGNORE INTO question_favorite (id, user_id, question_id, create_time, update_time)
            VALUES (#{id}, #{userId}, #{questionId}, #{createTime}, #{updateTime})
            """)
    int insertIgnore(QuestionFavorite favorite);

    int removeByUserIdAndQuestionId(@Param("userId") Long userId,
                                    @Param("questionId") Long questionId);

    List<QuestionFavoriteVo> findFavoriteList(@Param("userId") Long userId,
                                              @Param("cursor") Long cursor,
                                              @Param("limit") Integer limit,
                                              @Param("canViewMemberOnly") Integer canViewMemberOnly);
}
