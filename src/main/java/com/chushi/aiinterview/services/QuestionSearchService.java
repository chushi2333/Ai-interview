package com.chushi.aiinterview.services;

import com.chushi.aiinterview.entities.QuestionES;

import java.util.List;

public interface QuestionSearchService {
    List<QuestionES> searchQuestionByKeyword(String keyword, Integer difficulty, String tag, Integer page, Integer size);
}
