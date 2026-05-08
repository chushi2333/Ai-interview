package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.vo.QuestionFavoriteVo;
import com.chushi.aiinterview.entities.User;

import java.util.List;

public interface QuestionFavoriteService {
    void favoriteQuestion(Long questionId, User currentUser);

    void removeFavoriteQuestion(Long questionId, User currentUser);

    List<QuestionFavoriteVo> getFavoriteQuestionList(User currentUser, Long cursor, Integer limit);
}
