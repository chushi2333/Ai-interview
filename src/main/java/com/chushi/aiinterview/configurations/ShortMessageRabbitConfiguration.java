package com.chushi.aiinterview.configurations;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ShortMessageRabbitConfiguration {
    public static final String SHORT_MESSAGE_CODE_QUEUE_NAME = "sms.code.queue";
    public static final String SHORT_MESSAGE_CODE_EXCHANGE = "sms.code.exchange";
    public static final String SHORT_MESSAGE_CODE_ROUTING_KEY = "sms.code";

    @Bean
    public Queue rabbitSMSCodeQueue() {
        // 构造消息队列
        return QueueBuilder.durable(SHORT_MESSAGE_CODE_QUEUE_NAME).build();
    }

    @Bean
    public DirectExchange rabbitSMSCodeExchange() {
        // 构造交换机
        return ExchangeBuilder.directExchange(SHORT_MESSAGE_CODE_EXCHANGE).build();
    }

    @Bean
    public Binding shortMessageCodeBinding(Queue rabbitSMSCodeQueue, DirectExchange rabbitSMSCodeExchange) {
        // 绑定交换机和队列
        return BindingBuilder.bind(rabbitSMSCodeQueue).to(rabbitSMSCodeExchange).with(SHORT_MESSAGE_CODE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        // 配置消息转换器
        return new Jackson2JsonMessageConverter();
    }
}
