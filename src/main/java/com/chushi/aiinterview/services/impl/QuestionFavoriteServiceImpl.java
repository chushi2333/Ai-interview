package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.UserRoles;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.commons.vo.QuestionBankSourceVo;
import com.chushi.aiinterview.commons.vo.QuestionFavoriteVo;
import com.chushi.aiinterview.entities.Question;
import com.chushi.aiinterview.entities.QuestionFavorite;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.QuestionBankQuestionMapper;
import com.chushi.aiinterview.mappers.QuestionFavoriteMapper;
import com.chushi.aiinterview.mappers.QuestionMapper;
import com.chushi.aiinterview.services.QuestionFavoriteService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
public class QuestionFavoriteServiceImpl implements QuestionFavoriteService {
    @Resource
    private QuestionFavoriteMapper questionFavoriteMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionBankQuestionMapper questionBankQuestionMapper;

    @Resource
    private IdGenerator<Long> idGenerator;

    @Override
    @Transactional
    public void favoriteQuestion(Long questionId, User currentUser) {
        validateQuestionAccess(questionId, currentUser);
        var now = TimeUtils.currentLocalDateTime();
        // 收藏操作做成幂等，避免前端重复点击时抛错
        questionFavoriteMapper.insertIgnore(QuestionFavorite.builder()
                .id(idGenerator.nextId())
                .userId(currentUser.getId())
                .questionId(questionId)
                .createTime(now)
                .updateTime(now)
                .build());
    }

    @Override
    @Transactional
    public void removeFavoriteQuestion(Long questionId, User currentUser) {
        validateQuestionAccess(questionId, currentUser);
        // 取消收藏同样做成幂等，不要求前端先查状态再删
        questionFavoriteMapper.removeByUserIdAndQuestionId(currentUser.getId(), questionId);
    }

    @Override
    public List<QuestionFavoriteVo> getFavoriteQuestionList(User currentUser, Long cursor, Integer limit) {
        var userRoles = new UserRoles(currentUser.getRoles());
        var favoriteList = questionFavoriteMapper.findFavoriteList(
                currentUser.getId(),
                cursor,
                limit,
                userRoles.hasAny(UserRole.ADMIN, UserRole.SUPER_ADMIN) ? 1 : 0
        );

        if (favoriteList.isEmpty()) {
            return favoriteList;
        }

        var questionIds = favoriteList.stream().map(QuestionFavoriteVo::getQuestionId).distinct().toList();
        var bankSourceMap = new HashMap<Long, QuestionBankSourceVo>();
        questionBankQuestionMapper.findBankSourcesByQuestionIds(questionIds)
                .forEach(item -> bankSourceMap.put(item.getQuestionId(), item));

        favoriteList.forEach(item -> {
            var bankSource = bankSourceMap.get(item.getQuestionId());
            if (bankSource != null) {
                item.setQuestionBankId(bankSource.getQuestionBankId());
                item.setQuestionBankTitle(bankSource.getQuestionBankTitle());
            }
        });

        return favoriteList;
    }

    private Question validateQuestionAccess(Long questionId, User currentUser) {
        var question = questionMapper.findById(questionId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question not found")
        );
        if (question.getIsMemberOnly() != null && question.getIsMemberOnly() == 1) {
            var userRoles = new UserRoles(currentUser.getRoles());
            if (!userRoles.hasAny(UserRole.ADMIN, UserRole.SUPER_ADMIN)) {
                throw new BusinessException(HttpServletResponse.SC_FORBIDDEN, "Member only question");
            }
        }
        return question;
    }
}
