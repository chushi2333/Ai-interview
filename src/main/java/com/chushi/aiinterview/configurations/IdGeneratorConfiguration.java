package com.chushi.aiinterview.configurations;

import com.chushi.aiinterview.commons.utils.identifier.IdGenerator;
import com.chushi.aiinterview.commons.utils.identifier.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfiguration {
    @Value("1")
    private long machineId;

    @Bean
    public IdGenerator<Long> userIdGenerator() {
        return new SnowflakeIdGenerator(machineId);
    }
}
