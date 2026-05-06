package com.chushi.aiinterview.publishers;

import com.chushi.aiinterview.commons.utils.RabbitMessageData;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Slf4j
@Component
public class ESMessagePublisher {
    @Resource(name = "esRabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    public <T> void publishMessage(String exchangeName, String routingKey, RabbitMessageData<T> message) {
        // 事务提交后再发消息，避免数据库回滚后 ES 先更新成功
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                var correlationData = new CorrelationData(UUID.randomUUID().toString());
                try {
                    rabbitTemplate.convertAndSend(exchangeName, routingKey, message, correlationData);
                } catch (AmqpException e) {
                    log.error("RabbitMQ failed to publish message: {}", e.getMessage(), e);
                } catch (Exception e) {
                    log.error("Failed to publish ES message: {}", e.getMessage(), e);
                }
            }
        });
    }
}
