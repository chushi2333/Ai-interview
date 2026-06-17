package com.chushi.aiinterview.mappers;

import com.chushi.aiinterview.entities.QuestionSelfTestOption;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface QuestionSelfTestOptionMapper {
    int batchInsert(List<QuestionSelfTestOption> options);

    List<QuestionSelfTestOption> findBySelfTestIds(List<Long> selfTestIds);

    List<QuestionSelfTestOption> findBySelfTestId(Long selfTestId);

    int removeBySelfTestId(Long selfTestId);
}
