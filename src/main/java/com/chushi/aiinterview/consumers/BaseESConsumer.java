package com.chushi.aiinterview.consumers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;

import java.util.List;
import java.util.Map;

@Slf4j
public class BaseESConsumer {
    protected void handleDeadLetterMessage(Message message) {
        try {
            log.error("Received a dead letter queue message, data: {}", new String(message.getBody()));
            var deadInfo = message.getMessageProperties().getHeaders().get("x-death");
            if (deadInfo instanceof List<?> list) {
                for (var info : list) {
                    if (info instanceof Map<?, ?> map) {
                        var count = map.get("count");
                        var reason = map.get("reason");
                        log.error("Number of failed retries: {}, the reason for the failure: {}", count, reason);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process dead letter messages: {}", e.getMessage(), e);
        }
    }
}
