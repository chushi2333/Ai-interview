package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.vo.QuestionSearchItemVo;

import java.util.List;

public interface QuestionSearchService {
    List<QuestionSearchItemVo> searchQuestionByKeyword(String keyword, Integer difficulty, String tag, Integer page, Integer size);
}
