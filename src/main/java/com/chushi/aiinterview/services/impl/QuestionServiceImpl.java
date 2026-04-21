package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.dto.QuestionCreateDto;
import com.chushi.aiinterview.commons.dto.QuestionUpdateDto;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.UserRoles;
import com.chushi.aiinterview.commons.utils.cache.PreconfiguredRedisCacheTemplate;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.entities.Question;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.QuestionBankMapper;
import com.chushi.aiinterview.mappers.QuestionBankQuestionMapper;
import com.chushi.aiinterview.mappers.QuestionMapper;
import com.chushi.aiinterview.services.QuestionService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class QuestionServiceImpl implements QuestionService {
    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionBankMapper questionBankMapper;

    @Resource
    private QuestionBankQuestionMapper questionBankQuestionMapper;

    @Resource
    private IdGenerator<Long> idGenerator;

    @Resource
    private PreconfiguredRedisCacheTemplate<Long, Question> questionRedisCacheTemplate;

    @Override
    @Transactional
    public Question createQuestion(User creator, QuestionCreateDto question) {
        var now = TimeUtils.currentLocalDateTime();
        var questionEntity = Question.builder()
                .id(idGenerator.nextId())
                .title(question.getTitle())
                .content(question.getContent())
                .tags(question.getTags())
                .answer(question.getAnswer())
                .difficulty(question.getDifficulty() == null ? 1 : question.getDifficulty())
                .isMemberOnly(question.getIsMemberOnly() == null ? 0 : question.getIsMemberOnly())
                .userId(creator.getId())
                .editTime(now)
                .createTime(now)
                .updateTime(now)
                .isDelete(0)
                .build();

        try {
            var affectedRows = questionMapper.insert(questionEntity);
            if (affectedRows != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Create question failed");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("CreateQuestionException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Create question failed");
        }

        return questionEntity;
    }

    @Override
    public Question getQuestionById(Long questionId, User currentUser) {
        // 详情先走缓存，权限判断仍然放在业务层处理
        var question = questionRedisCacheTemplate.queryById(questionId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question not found")
        );
        // 会员题只允许会员和管理员查看详情
        if (question.getIsMemberOnly() != null && question.getIsMemberOnly() == 1) {
            var userRoles = new UserRoles(currentUser.getRoles());
            if (!userRoles.hasAny(UserRole.ADMIN, UserRole.SUPER_ADMIN)) {
                throw new BusinessException(HttpServletResponse.SC_FORBIDDEN, "Member only question");
            }
        }
        return question;
    }

    @Override
    public List<Question> getQuestionList(Long cursor, Integer limit, Long userId) {
        return questionMapper.findQuestionList(cursor, limit, userId);
    }

    @Override
    public List<Question> getQuestionListByQuestionBankId(Long questionBankId, Long cursor, Integer limit) {
        // 题库公开可见，但列表查询前仍然要确认题库存在
        questionBankMapper.findById(questionBankId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question bank not found")
        );
        return questionMapper.findQuestionListByQuestionBankId(questionBankId, cursor, limit);
    }

    @Override
    @Transactional
    public void updateQuestion(Long questionId, Long userId, QuestionUpdateDto question) {
        var questionEntity = questionMapper.findById(questionId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question not found")
        );
        var now = TimeUtils.currentLocalDateTime();

        questionEntity.setTitle(question.getTitle());
        questionEntity.setContent(question.getContent());
        questionEntity.setTags(question.getTags());
        questionEntity.setAnswer(question.getAnswer());
        questionEntity.setDifficulty(question.getDifficulty() == null ? 1 : question.getDifficulty());
        questionEntity.setIsMemberOnly(question.getIsMemberOnly() == null ? 0 : question.getIsMemberOnly());
        questionEntity.setEditTime(now);
        questionEntity.setUpdateTime(now);

        try {
            var affectedRows = questionMapper.updateQuestion(questionEntity);
            if (affectedRows != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Update question failed");
            }
            questionRedisCacheTemplate.removeCache(questionId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("UpdateQuestionException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Update question failed");
        }
    }

    @Override
    @Transactional
    public void removeQuestion(Long questionId, Long userId) {
        // 先确认题目存在，再做删除和关联清理
        questionMapper.findById(questionId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question not found")
        );

        try {
            var affectedRows = questionMapper.removeQuestion(questionId, TimeUtils.currentLocalDateTime());
            if (affectedRows != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Remove question failed");
            }
            // 手动清理题库和题目的关联关系
            questionBankQuestionMapper.removeByQuestionId(questionId);
            questionRedisCacheTemplate.removeCache(questionId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("RemoveQuestionException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Remove question failed");
        }
    }
}
