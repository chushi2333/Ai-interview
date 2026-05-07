package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.commons.dto.QuestionSelfTestCreateDto;
import com.chushi.aiinterview.commons.dto.QuestionSelfTestSubmitDto;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.utils.QuestionSelfTestAnswerUtils;
import com.chushi.aiinterview.commons.utils.TimeUtils;
import com.chushi.aiinterview.commons.utils.UserRoles;
import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestListVo;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestManageOptionVo;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestManageVo;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestOptionVo;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestSubmitResultVo;
import com.chushi.aiinterview.commons.vo.QuestionSelfTestVo;
import com.chushi.aiinterview.entities.Question;
import com.chushi.aiinterview.entities.QuestionPracticeRecord;
import com.chushi.aiinterview.entities.QuestionSelfTest;
import com.chushi.aiinterview.entities.QuestionSelfTestOption;
import com.chushi.aiinterview.entities.QuestionSelfTestRecord;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.mappers.QuestionMapper;
import com.chushi.aiinterview.mappers.QuestionPracticeRecordMapper;
import com.chushi.aiinterview.mappers.QuestionSelfTestMapper;
import com.chushi.aiinterview.mappers.QuestionSelfTestOptionMapper;
import com.chushi.aiinterview.mappers.QuestionSelfTestRecordMapper;
import com.chushi.aiinterview.services.QuestionSelfTestService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class QuestionSelfTestServiceImpl implements QuestionSelfTestService {
    @Resource
    private QuestionSelfTestMapper questionSelfTestMapper;

    @Resource
    private QuestionSelfTestOptionMapper questionSelfTestOptionMapper;

    @Resource
    private QuestionSelfTestRecordMapper questionSelfTestRecordMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionPracticeRecordMapper questionPracticeRecordMapper;

    @Resource
    private IdGenerator<Long> idGenerator;

    @Override
    @Transactional
    public QuestionSelfTestManageVo createSelfTest(Long questionId, QuestionSelfTestCreateDto selfTest) {
        questionMapper.findById(questionId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Question not found")
        );
        // 第一版只做单选题，所以这里重点校验唯一正确选项和选项标识重复
        validateCreateSelfTest(selfTest);

        var now = TimeUtils.currentLocalDateTime();
        var selfTestEntity = QuestionSelfTest.builder()
                .id(idGenerator.nextId())
                .questionId(questionId)
                .content(selfTest.getContent())
                .explanation(selfTest.getExplanation())
                .createTime(now)
                .updateTime(now)
                .isDelete(0)
                .build();

        var optionEntities = new ArrayList<QuestionSelfTestOption>();
        // 先把选项标准化成独立实体，后面查题和判题都按选项表处理
        for (var option : selfTest.getOptions()) {
            optionEntities.add(QuestionSelfTestOption.builder()
                    .id(idGenerator.nextId())
                    .selfTestId(selfTestEntity.getId())
                    .optionKey(normalizeOptionKey(option.getOptionKey()))
                    .content(option.getContent())
                    .isCorrect(option.getIsCorrect())
                    .sortOrder(option.getSortOrder())
                    .createTime(now)
                    .updateTime(now)
                    .build());
        }

        try {
            if (questionSelfTestMapper.insert(selfTestEntity) != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Create self test failed");
            }
            if (questionSelfTestOptionMapper.batchInsert(optionEntities) != optionEntities.size()) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Create self test options failed");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("CreateQuestionSelfTestException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Create self test failed");
        }

        return buildManageVo(selfTestEntity, optionEntities);
    }

    @Override
    public QuestionSelfTestListVo getSelfTestsByQuestionId(Long questionId, User currentUser) {
        validateQuestionAccess(questionId, currentUser);

        var selfTests = questionSelfTestMapper.findByQuestionId(questionId);
        if (selfTests.isEmpty()) {
            return new QuestionSelfTestListVo(List.of());
        }

        var selfTestIds = selfTests.stream().map(QuestionSelfTest::getId).toList();
        var optionEntities = questionSelfTestOptionMapper.findBySelfTestIds(selfTestIds);
        var optionMap = groupOptionsBySelfTestId(optionEntities);

        // 用户侧只返回做题所需信息，不暴露正确答案
        var tests = selfTests.stream()
                .map(selfTest -> QuestionSelfTestVo.builder()
                        .id(selfTest.getId())
                        .questionId(selfTest.getQuestionId())
                        .content(selfTest.getContent())
                        .options(buildUserOptionVos(optionMap.getOrDefault(selfTest.getId(), List.of())))
                        .build())
                .toList();
        return new QuestionSelfTestListVo(tests);
    }

    @Override
    @Transactional
    public QuestionSelfTestSubmitResultVo submitSelfTest(Long selfTestId, User currentUser, QuestionSelfTestSubmitDto submitDto) {
        var selfTest = questionSelfTestMapper.findById(selfTestId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Self test not found")
        );
        var question = validateQuestionAccess(selfTest.getQuestionId(), currentUser);
        var optionEntities = questionSelfTestOptionMapper.findBySelfTestId(selfTestId);
        if (optionEntities.isEmpty()) {
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Self test options not found");
        }

        var selectedAnswer = normalizeOptionKey(submitDto.getSelectedAnswer());
        validateSubmitAnswer(selectedAnswer, optionEntities);

        // 单选题只会有一个正确答案
        var correctAnswer = optionEntities.stream()
                .filter(option -> option.getIsCorrect() != null && option.getIsCorrect() == 1)
                .map(QuestionSelfTestOption::getOptionKey)
                .findFirst()
                .orElseThrow(() -> new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Correct option not found"));
        var isCorrect = selectedAnswer.equals(correctAnswer) ? 1 : 0;
        var now = TimeUtils.currentLocalDateTime();

        // TODO: 后续接入 AI 时，可以在自测结果之外再叠加更细的学习分析和推荐
        var selfTestRecord = QuestionSelfTestRecord.builder()
                .id(idGenerator.nextId())
                .selfTestId(selfTestId)
                .userId(currentUser.getId())
                .selectedAnswers(QuestionSelfTestAnswerUtils.toJson(List.of(selectedAnswer)))
                .isCorrect(isCorrect)
                .durationSeconds(submitDto.getDurationSeconds())
                .createTime(now)
                .updateTime(now)
                .build();

        // 先保留一条按题聚合的过渡记录，保证现有热力图和统计链路不断
        var practiceRecord = QuestionPracticeRecord.builder()
                .id(idGenerator.nextId())
                .userId(currentUser.getId())
                .questionId(question.getId())
                .isCorrect(isCorrect)
                .durationSeconds(submitDto.getDurationSeconds())
                .practiceDate(now.toLocalDate())
                .createTime(now)
                .updateTime(now)
                .build();

        try {
            if (questionSelfTestRecordMapper.insert(selfTestRecord) != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Submit self test failed");
            }
            if (questionPracticeRecordMapper.insert(practiceRecord) != 1) {
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Create practice record failed");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("SubmitQuestionSelfTestException: {}", e.getMessage(), e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Submit self test failed");
        }

        return QuestionSelfTestSubmitResultVo.builder()
                .recordId(selfTestRecord.getId())
                .selfTestId(selfTestId)
                .questionId(question.getId())
                .isCorrect(isCorrect)
                .selectedAnswer(selectedAnswer)
                .correctAnswer(correctAnswer)
                .explanation(selfTest.getExplanation())
                .build();
    }

    @Override
    @Transactional
    public void removeSelfTest(Long selfTestId) {
        questionSelfTestMapper.findById(selfTestId).orElseThrow(
                () -> new BusinessException(HttpServletResponse.SC_NOT_FOUND, "Self test not found")
        );
        var affectedRows = questionSelfTestMapper.removeById(selfTestId, TimeUtils.currentLocalDateTime());
        if (affectedRows != 1) {
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Remove self test failed");
        }
        questionSelfTestOptionMapper.removeBySelfTestId(selfTestId);
    }

    private void validateCreateSelfTest(QuestionSelfTestCreateDto selfTest) {
        var normalizedKeys = new HashSet<String>();
        var correctCount = 0;
        // 单选题创建时同时校验“选项标识不重复”和“只有一个正确答案”
        for (var option : selfTest.getOptions()) {
            var optionKey = normalizeOptionKey(option.getOptionKey());
            if (!normalizedKeys.add(optionKey)) {
                throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "Duplicate option key");
            }
            if (option.getIsCorrect() != null && option.getIsCorrect() == 1) {
                correctCount++;
            }
        }

        if (correctCount != 1) {
            throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "Single choice must have exactly one correct option");
        }
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

    private void validateSubmitAnswer(String selectedAnswer,
                                      List<QuestionSelfTestOption> optionEntities) {
        // 提交答案必须是当前题目真实存在的选项标识
        var validOptionKeys = optionEntities.stream()
                .map(QuestionSelfTestOption::getOptionKey)
                .map(this::normalizeOptionKey)
                .collect(java.util.stream.Collectors.toSet());
        if (!validOptionKeys.contains(selectedAnswer)) {
            throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "Invalid selected answer");
        }
    }

    private String normalizeOptionKey(String optionKey) {
        return optionKey == null ? null : optionKey.trim().toUpperCase(Locale.ROOT);
    }

    private Map<Long, List<QuestionSelfTestOption>> groupOptionsBySelfTestId(List<QuestionSelfTestOption> optionEntities) {
        var optionMap = new HashMap<Long, List<QuestionSelfTestOption>>();
        for (var option : optionEntities) {
            optionMap.computeIfAbsent(option.getSelfTestId(), key -> new ArrayList<>()).add(option);
        }
        // 同一道题的选项按显示顺序稳定返回，避免前端每次渲染顺序漂移
        for (var entry : optionMap.entrySet()) {
            entry.getValue().sort(Comparator.comparing(QuestionSelfTestOption::getSortOrder).thenComparing(QuestionSelfTestOption::getId));
        }
        return optionMap;
    }

    private List<QuestionSelfTestOptionVo> buildUserOptionVos(List<QuestionSelfTestOption> optionEntities) {
        return optionEntities.stream()
                .map(option -> QuestionSelfTestOptionVo.builder()
                        .id(option.getId())
                        .optionKey(option.getOptionKey())
                        .content(option.getContent())
                        .sortOrder(option.getSortOrder())
                        .build())
                .toList();
    }

    private QuestionSelfTestManageVo buildManageVo(QuestionSelfTest selfTest, List<QuestionSelfTestOption> optionEntities) {
        var options = optionEntities.stream()
                .map(option -> QuestionSelfTestManageOptionVo.builder()
                        .id(option.getId())
                        .optionKey(option.getOptionKey())
                        .content(option.getContent())
                        .isCorrect(option.getIsCorrect())
                        .sortOrder(option.getSortOrder())
                        .build())
                .toList();

        return QuestionSelfTestManageVo.builder()
                .id(selfTest.getId())
                .questionId(selfTest.getQuestionId())
                .content(selfTest.getContent())
                .explanation(selfTest.getExplanation())
                .options(options)
                .build();
    }
}
