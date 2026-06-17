package com.chushi.aiinterview.repository;

import com.chushi.aiinterview.entities.QuestionES;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface QuestionRepository extends ElasticsearchRepository<QuestionES, Long> {
}
