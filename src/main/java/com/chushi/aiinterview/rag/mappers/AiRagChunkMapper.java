package com.chushi.aiinterview.rag.mappers;

import com.chushi.aiinterview.commons.vo.AiRagChunkSearchVo;
import com.chushi.aiinterview.entities.AiRagChunk;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AiRagChunkMapper {
    int insert(AiRagChunk chunk);

    int deleteByQuestionId(@Param("questionId") Long questionId);

    int countByQuestionId(@Param("questionId") Long questionId);

    List<AiRagChunkSearchVo> searchTopK(@Param("embedding") String embedding,
                                        @Param("limit") Integer limit);
}
