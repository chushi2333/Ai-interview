package com.chushi.aiinterview.consumers;

import com.chushi.aiinterview.commons.utils.RabbitMessageData;
import com.chushi.aiinterview.configurations.QuestionRabbitConfiguration;
import com.chushi.aiinterview.entities.QuestionES;
import com.chushi.aiinterview.repository.QuestionRepository;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class QuestionConsumer extends BaseESConsumer {
    @Resource
    private QuestionRepository questionRepository;

    @RabbitListener(queues = QuestionRabbitConfiguration.QUESTION_ELASTICSEARCH_QUEUE_NAME)
    public void consumeQuestionMessage(RabbitMessageData<QuestionES> questionMessage) {
        switch (questionMessage.getAction()) {
            case CREATE, UPDATE -> questionRepository.save(questionMessage.getPayload());
            case DELETE -> questionRepository.deleteById(questionMessage.getBusinessId());
        }
    }

    @RabbitListener(queues = QuestionRabbitConfiguration.QUESTION_DEAD_LETTER_QUEUE_NAME)
    public void consumeDeadLetterMessage(Message message) {
        handleDeadLetterMessage(message);
    }
}
