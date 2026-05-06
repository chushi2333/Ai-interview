package com.chushi.aiinterview.services.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.chushi.aiinterview.entities.QuestionES;
import com.chushi.aiinterview.services.QuestionSearchService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionSearchServiceImpl implements QuestionSearchService {
    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public List<QuestionES> searchQuestionByKeyword(String keyword, Integer difficulty, Integer page, Integer size) {
        var pageable = PageRequest.of(page, size);
        var nativeQueryBuilder = NativeQuery.builder()
                // 关键词命中标题、内容、答案、标签任一字段即可
                .withQuery(query -> query.bool(boolQuery -> {
                    boolQuery.should(matchQuery -> matchQuery.match(match -> match.field("title").query(keyword).boost(3.0f)));
                    boolQuery.should(matchQuery -> matchQuery.match(match -> match.field("content").query(keyword).boost(2.0f)));
                    boolQuery.should(matchQuery -> matchQuery.match(match -> match.field("answer").query(keyword).boost(2.0f)));
                    boolQuery.should(matchQuery -> matchQuery.match(match -> match.field("tags").query(keyword).boost(2.0f)));
                    boolQuery.minimumShouldMatch("1");
                    if (difficulty != null) {
                        boolQuery.filter(filterQuery ->
                                filterQuery.term(termQuery -> termQuery.field("difficulty").value(FieldValue.of(difficulty))));
                    }
                    return boolQuery;
                }))
                .withSort(sort -> sort.score(score -> score.order(SortOrder.Desc)))
                .withSort(sort -> sort.field(field -> field.field("createdAt").order(SortOrder.Desc)))
                .withPageable(pageable);
        var query = nativeQueryBuilder.build();
        var searchHits = elasticsearchOperations.search(query, QuestionES.class);
        return searchHits.stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
