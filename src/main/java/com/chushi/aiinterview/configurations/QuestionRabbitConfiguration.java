package com.chushi.aiinterview.configurations;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class QuestionRabbitConfiguration {
    public static final String QUESTION_ELASTICSEARCH_QUEUE_NAME = "question.es.queue";
    public static final String QUESTION_ELASTICSEARCH_EXCHANGE = "question.es.exchange";
    public static final String QUESTION_ELASTICSEARCH_ROUTING_KEY = "question.es";

    public static final String QUESTION_DEAD_LETTER_QUEUE_NAME = "question.dead.queue";
    public static final String QUESTION_DEAD_LETTER_EXCHANGE = "question.dead.exchange";
    public static final String QUESTION_DEAD_LETTER_ROUTING_KEY = "question.dead";

    @Bean
    public Queue rabbitQuestionESQueue() {
        Map<String, Object> args = new HashMap<>(16);
        args.put("x-dead-letter-exchange", QUESTION_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", QUESTION_DEAD_LETTER_ROUTING_KEY);
        return QueueBuilder.durable(QUESTION_ELASTICSEARCH_QUEUE_NAME).withArguments(args).build();
    }

    @Bean
    public DirectExchange rabbitQuestionESExchange() {
        return ExchangeBuilder.directExchange(QUESTION_ELASTICSEARCH_EXCHANGE).build();
    }

    @Bean
    public Binding rabbitQuestionESBinding(Queue rabbitQuestionESQueue, DirectExchange rabbitQuestionESExchange) {
        return BindingBuilder.bind(rabbitQuestionESQueue)
                .to(rabbitQuestionESExchange)
                .with(QUESTION_ELASTICSEARCH_ROUTING_KEY);
    }

    @Bean
    public Queue rabbitQuestionDeadLetterQueue() {
        return QueueBuilder.durable(QUESTION_DEAD_LETTER_QUEUE_NAME).build();
    }

    @Bean
    public DirectExchange rabbitQuestionDeadLetterExchange() {
        return ExchangeBuilder.directExchange(QUESTION_DEAD_LETTER_EXCHANGE).build();
    }

    @Bean
    public Binding rabbitQuestionDeadLetterBinding(Queue rabbitQuestionDeadLetterQueue,
                                                   DirectExchange rabbitQuestionDeadLetterExchange) {
        return BindingBuilder.bind(rabbitQuestionDeadLetterQueue)
                .to(rabbitQuestionDeadLetterExchange)
                .with(QUESTION_DEAD_LETTER_ROUTING_KEY);
    }
}
