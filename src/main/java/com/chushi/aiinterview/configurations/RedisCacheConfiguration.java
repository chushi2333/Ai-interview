package com.chushi.aiinterview.configurations;

import com.chushi.aiinterview.commons.utils.cache.PreconfiguredRedisCacheTemplate;
import com.chushi.aiinterview.entities.Question;
import com.chushi.aiinterview.entities.QuestionBank;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.mappers.QuestionBankMapper;
import com.chushi.aiinterview.mappers.QuestionMapper;
import com.chushi.aiinterview.mappers.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisCacheConfiguration extends BaseRedisCacheUtilConfiguration {
    @Resource
    private UserMapper userMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionBankMapper questionBankMapper;

    @Bean
    public PreconfiguredRedisCacheTemplate<Long, User> userRedisTemplate() {
        return new PreconfiguredRedisCacheTemplate<>(
                "user",
                3600L,
                userMapper::findById,
                User.class
        );
    }

    @Bean
    public PreconfiguredRedisCacheTemplate<Long, Question> questionRedisTemplate() {
        return new PreconfiguredRedisCacheTemplate<>(
                "question",
                3600L,
                questionMapper::findById,
                Question.class
        );
    }

    @Bean
    public PreconfiguredRedisCacheTemplate<Long, QuestionBank> questionBankRedisTemplate() {
        return new PreconfiguredRedisCacheTemplate<>(
                "questionBank",
                3600L,
                questionBankMapper::findById,
                QuestionBank.class
        );
    }
}
