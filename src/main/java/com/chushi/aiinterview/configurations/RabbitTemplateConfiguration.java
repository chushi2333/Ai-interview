package com.chushi.aiinterview.configurations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitTemplateConfiguration {
    @Bean(name = "esRabbitTemplate")
    public RabbitTemplate esRabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        var rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        // ES 同步链路需要知道“消息是否真的到达交换机”和“是否成功路由到队列”
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("RabbitMQ publisher confirm success: correlationId={}",
                        correlationData == null ? null : correlationData.getId());
                return;
            }
            log.error("RabbitMQ publisher confirm failed: correlationId={}, cause={}",
                    correlationData == null ? null : correlationData.getId(),
                    cause);
        });
        rabbitTemplate.setReturnsCallback(returned -> log.error(
                "RabbitMQ message returned: exchange={}, routingKey={}, replyCode={}, replyText={}, message={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText(),
                new String(returned.getMessage().getBody())
        ));
        return rabbitTemplate;
    }
}
