package com.chushi.aiinterview.commons.utils;

import com.chushi.aiinterview.commons.enums.RabbitMessageAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RabbitMessageData<T> {
    private T payload;
    private Long businessId;
    private RabbitMessageAction action;
}
