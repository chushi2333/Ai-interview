package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.dto.AiRagQuestionBatchIndexDto;
import com.chushi.aiinterview.commons.dto.AiRagSearchDebugDto;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.vo.AiRagQuestionBatchIndexVo;
import com.chushi.aiinterview.commons.vo.AiRagQuestionIndexVo;
import com.chushi.aiinterview.commons.vo.AiRagSearchDebugVo;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.services.AiRagIndexService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiRagController extends BaseController {
    @Resource
    private AiRagIndexService aiRagIndexService;

    @PostMapping("/api/ai/rag/search/debug")
    @Operation(summary = "调试 RAG 向量检索")
    @RequireRole(value = {UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<AiRagSearchDebugVo> searchDebug(@Valid @RequestBody AiRagSearchDebugDto request) {
        return wrap(aiRagIndexService.searchDebug(request.getQuery(), request.getTopK()));
    }


    @PostMapping("/api/ai/rag/questions/index/batch")
    @Operation(summary = "批量重建题目 RAG 索引")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<AiRagQuestionBatchIndexVo> rebuildQuestionIndexBatch(@Valid @RequestBody AiRagQuestionBatchIndexDto request) {
        return wrap(aiRagIndexService.rebuildQuestionIndexBatch(request.getQuestionIds(), request.getLimit()));
    }

    @PostMapping("/api/ai/rag/questions/{questionId}/index")
    @Operation(summary = "重建题目 RAG 索引")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<AiRagQuestionIndexVo> rebuildQuestionIndex(@PathVariable Long questionId) {
        return wrap(aiRagIndexService.rebuildQuestionIndex(questionId));
    }
}
