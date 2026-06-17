package com.chushi.aiinterview.services.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import com.chushi.aiinterview.commons.vo.QuestionBankSourceVo;
import com.chushi.aiinterview.commons.vo.QuestionSearchItemVo;
import com.chushi.aiinterview.entities.QuestionES;
import com.chushi.aiinterview.mappers.QuestionBankQuestionMapper;
import com.chushi.aiinterview.services.QuestionSearchService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class QuestionSearchServiceImpl implements QuestionSearchService {
    @Resource
    private ElasticsearchOperations elasticsearchOperations;

    @Resource
    private QuestionBankQuestionMapper questionBankQuestionMapper;

    @Override
    public List<QuestionSearchItemVo> searchQuestionByKeyword(String keyword, Integer difficulty, String tag, Integer page, Integer size) {
        var pageable = PageRequest.of(page, size);
        // TODO: 后续在这里继续扩中文分词、容错模糊和高亮能力
        var nativeQueryBuilder = NativeQuery.builder()
                // 标题、摘要和标签代表题目主题，权重更高；题解正文也参与搜索，但降低权重，
                // 避免代码示例或延伸说明里的偶然词把无关题目排到前面。
                .withQuery(query -> query.bool(boolQuery -> {
                    boolQuery.should(matchQuery -> matchQuery.match(match -> match.field("title").query(keyword).boost(4.0f)));
                    boolQuery.should(matchQuery -> matchQuery.match(match -> match.field("content").query(keyword).boost(2.5f)));
                    boolQuery.should(matchQuery -> matchQuery.match(match -> match.field("tags").query(keyword).boost(3.0f)));
                    boolQuery.should(matchQuery -> matchQuery.match(match -> match.field("answer").query(keyword).boost(0.35f)));
                    boolQuery.minimumShouldMatch("1");
                    if (difficulty != null) {
                        boolQuery.filter(filterQuery ->
                                filterQuery.term(termQuery -> termQuery.field("difficulty").value(FieldValue.of(difficulty))));
                    }
                    // tags 在 ES 里是 keyword 数组，这里按单个标签做精确过滤
                    if (tag != null && !tag.isBlank()) {
                        boolQuery.filter(filterQuery ->
                                filterQuery.term(termQuery -> termQuery.field("tags").value(FieldValue.of(tag))));
                    }
                    return boolQuery;
                }))
                .withSort(sort -> sort.score(score -> score.order(SortOrder.Desc)))
                .withSort(sort -> sort.field(field -> field.field("createdAt").order(SortOrder.Desc)))
                .withPageable(pageable);
        var query = nativeQueryBuilder.build();
        var searchHits = elasticsearchOperations.search(query, QuestionES.class);
        var questions = searchHits.stream()
                .map(SearchHit::getContent)
                .toList();

        if (questions.isEmpty()) {
            return List.of();
        }

        var questionIds = questions.stream().map(QuestionES::getId).distinct().toList();
        var bankSourceMap = new HashMap<Long, QuestionBankSourceVo>();
        for (var bankSource : questionBankQuestionMapper.findBankSourcesByQuestionIds(questionIds)) {
            bankSourceMap.put(bankSource.getQuestionId(), bankSource);
        }

        return questions.stream()
                .map(question -> {
                    var bankSource = bankSourceMap.get(question.getId());
                    return QuestionSearchItemVo.builder()
                            .id(question.getId())
                            .title(question.getTitle())
                            .content(question.getContent())
                            .answer(question.getAnswer())
                            .tags(question.getTags())
                            .difficulty(question.getDifficulty())
                            .isMemberOnly(question.getIsMemberOnly())
                            .userId(question.getUserId())
                            .questionBankId(bankSource == null ? null : bankSource.getQuestionBankId())
                            .questionBankTitle(bankSource == null ? null : bankSource.getQuestionBankTitle())
                            .createdAt(question.getCreatedAt())
                            .build();
                })
                .toList();
    }
}
