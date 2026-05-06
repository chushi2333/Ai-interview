package com.chushi.aiinterview.configurations;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTemplateConfiguration {
    @Bean(name = "esRabbitTemplate")
    public RabbitTemplate esRabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        // 先单独保留 ES 消息模板入口，后续再补 confirm / return 回调
        var rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
