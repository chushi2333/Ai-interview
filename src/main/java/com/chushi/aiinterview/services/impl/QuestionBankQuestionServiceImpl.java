package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.entities.QuestionBankQuestion;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.QuestionBankQuestionMapper;
import com.chushi.aiinterview.mappers.QuestionBankMapper;
import com.chushi.aiinterview.mappers.QuestionMapper;
import com.chushi.aiinterview.services.QuestionBankQuestionService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Slf4j
public class QuestionBankQuestionServiceImpl implements QuestionBankQuestionService {
    @Resource
    private QuestionBankQuestionMapper questionBankQuestionMapper;

    @Resource
    private QuestionBankMapper questionBankMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private IdGenerator<Long> idGenerator;

    @Override
    @Transactional
    public void addQuestionToBank(Long questionBankId, Long questionId, Long userId) {
        // 官方题库只需要确认资源存在
        questionBankMapper.findById(questionBankId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question bank not found")
        );

        questionMapper.findById(questionId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question not found")
        );

        if (questionBankQuestionMapper.countByQuestionBankIdAndQuestionId(questionBankId, questionId) > 0) {
            throw new BusinessException(HttpServletResponse.SC_CONFLICT, "Question already exists in question bank");
        }

        var now = TimeUtils.currentLocalDateTime();
        var relation = QuestionBankQuestion.builder()
                .id(idGenerator.nextId())
                .questionBankId(questionBankId)
                .questionId(questionId)
                .userId(userId)
                .createTime(now)
                .updateTime(now)
                .build();

        try {
            var affectedRows = questionBankQuestionMapper.insert(relation);
            if (affectedRows != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Add question to question bank failed");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AddQuestionToBankException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Add question to question bank failed");
        }
    }

    @Override
    @Transactional
    public int addQuestionsToBank(Long questionBankId, List<Long> questionIds, Long userId) {
        if (questionIds == null || questionIds.isEmpty()) {
            return 0;
        }

        // 批量添加前先确认题库存在
        questionBankMapper.findById(questionBankId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question bank not found")
        );

        var now = TimeUtils.currentLocalDateTime();
        var relations = new ArrayList<QuestionBankQuestion>();
        // 先去重，避免同一批请求里的重复题目触发唯一索引冲突
        for (var questionId : new LinkedHashSet<>(questionIds)) {
            questionMapper.findById(questionId).orElseThrow(
                    () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question not found")
            );

            if (questionBankQuestionMapper.countByQuestionBankIdAndQuestionId(questionBankId, questionId) > 0) {
                continue;
            }

            relations.add(QuestionBankQuestion.builder()
                    .id(idGenerator.nextId())
                    .questionBankId(questionBankId)
                    .questionId(questionId)
                    .userId(userId)
                    .createTime(now)
                    .updateTime(now)
                    .build());
        }

        if (relations.isEmpty()) {
            return 0;
        }

        try {
            questionBankQuestionMapper.insertBatch(relations);
            return relations.size();
        } catch (Exception e) {
            log.error("AddQuestionsToBankException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Batch add questions to question bank failed");
        }
    }

    @Override
    @Transactional
    public void removeQuestionFromBank(Long questionBankId, Long questionId, Long userId) {
        // 删除关系前先确认题库存在
        questionBankMapper.findById(questionBankId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question bank not found")
        );

        try {
            var affectedRows = questionBankQuestionMapper.removeByQuestionBankIdAndQuestionId(questionBankId, questionId);
            if (affectedRows != 1) {
                throw new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question not found in question bank");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("RemoveQuestionFromBankException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Remove question from question bank failed");
        }
    }
}
