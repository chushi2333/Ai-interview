package com.chushi.aiinterview.consumers;

import com.chushi.aiinterview.commons.dto.ShortMessageCodeDto;
import com.chushi.aiinterview.configurations.ShortMessageRabbitConfiguration;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ShortMessageConsumer {
    @RabbitListener(queues = ShortMessageRabbitConfiguration.SHORT_MESSAGE_CODE_QUEUE_NAME)
    public void handleShortMessageCode(ShortMessageCodeDto shortMessageCodeDto) {
        // TODO: 替换为调用 SMS 发送接口
        System.out.println(shortMessageCodeDto.getPhoneNumber() + " 的验证码为: " + shortMessageCodeDto.getCode());
    }
}
