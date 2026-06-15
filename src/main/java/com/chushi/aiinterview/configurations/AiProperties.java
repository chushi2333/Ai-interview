package com.chushi.aiinterview.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private ChatModelProperties chatModel = new ChatModelProperties();

    @Data
    public static class ChatModelProperties {
        private String apiKey;

        private String baseUrl;

        private String modelName;

        private Double temperature;

        private java.time.Duration timeout;

        private Boolean logRequests;

        private Boolean logResponses;
    }
}
