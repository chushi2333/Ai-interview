package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.dto.QuestionBankCreateDto;
import com.chushi.aiinterview.commons.dto.QuestionBankUpdateDto;
import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.entities.QuestionBank;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.QuestionBankQuestionMapper;
import com.chushi.aiinterview.mappers.QuestionBankMapper;
import com.chushi.aiinterview.services.QuestionBankService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class QuestionBankServiceImpl implements QuestionBankService {
    @Resource
    private QuestionBankMapper questionBankMapper;

    @Resource
    private QuestionBankQuestionMapper questionBankQuestionMapper;

    @Resource
    private IdGenerator<Long> idGenerator;

    @Override
    @Transactional
    public QuestionBank createQuestionBank(User creator, QuestionBankCreateDto questionBank) {
        var now = TimeUtils.currentLocalDateTime();
        var questionBankEntity = QuestionBank.builder()
                .id(idGenerator.nextId())
                .title(questionBank.getTitle())
                .description(questionBank.getDescription())
                .picture(questionBank.getPicture())
                .userId(creator.getId())
                .editTime(now)
                .createTime(now)
                .updateTime(now)
                .isDelete(0)
                .build();

        try {
            var affectedRows = questionBankMapper.insert(questionBankEntity);
            if (affectedRows != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Create question bank failed");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("CreateQuestionBankException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Create question bank failed");
        }

        return questionBankEntity;
    }

    @Override
    public QuestionBank getQuestionBankById(Long questionBankId) {
        return questionBankMapper.findById(questionBankId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question bank not found")
        );
    }

    @Override
    public List<QuestionBank> getQuestionBankList(Long cursor, Integer limit, Long userId) {
        return questionBankMapper.findQuestionBankList(cursor, limit, userId);
    }

    @Override
    @Transactional
    public void updateQuestionBank(Long questionBankId, Long userId, QuestionBankUpdateDto questionBank) {
        var questionBankEntity = questionBankMapper.findById(questionBankId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question bank not found")
        );
        var now = TimeUtils.currentLocalDateTime();

        questionBankEntity.setTitle(questionBank.getTitle());
        questionBankEntity.setDescription(questionBank.getDescription());
        questionBankEntity.setPicture(questionBank.getPicture());
        questionBankEntity.setEditTime(now);
        questionBankEntity.setUpdateTime(now);

        try {
            var affectedRows = questionBankMapper.updateQuestionBank(questionBankEntity);
            if (affectedRows != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Update question bank failed");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("UpdateQuestionBankException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Update question bank failed");
        }
    }

    @Override
    @Transactional
    public void removeQuestionBank(Long questionBankId, Long userId) {
        // 先确认题库存在，再做删除和关联清理
        questionBankMapper.findById(questionBankId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question bank not found")
        );
        try {
            var affectedRows = questionBankMapper.removeQuestionBank(questionBankId, TimeUtils.currentLocalDateTime());
            if (affectedRows != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Remove question bank failed");
            }
            // 手动清理题库和题目的关联关系
            questionBankQuestionMapper.removeByQuestionBankId(questionBankId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("RemoveQuestionBankException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Remove question bank failed");
        }
    }
}
