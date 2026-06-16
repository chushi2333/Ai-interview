package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.vo.AiRagQuestionBatchIndexVo;
import com.chushi.aiinterview.commons.vo.AiRagQuestionIndexVo;
import com.chushi.aiinterview.commons.vo.AiRagSearchDebugVo;

public interface AiRagIndexService {
    AiRagQuestionIndexVo rebuildQuestionIndex(Long questionId);

    AiRagQuestionBatchIndexVo rebuildQuestionIndexBatch(java.util.List<Long> questionIds, Integer limit);

    AiRagSearchDebugVo searchDebug(String query, Integer topK);
}
