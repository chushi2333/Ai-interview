package com.chushi.aiinterview.components;

import com.chushi.aiinterview.configurations.AiProperties;
import com.chushi.aiinterview.exceptions.BusinessException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiChatModelProvider {
    @Resource
    private AiProperties aiProperties;

    private volatile ChatModel chatModel;

    public ChatModel getChatModel() {
        var configuredChatModel = chatModel;
        if (configuredChatModel != null) {
            return configuredChatModel;
        }

        synchronized (this) {
            if (chatModel == null) {
                var config = aiProperties.getChatModel();
                if (!StringUtils.hasText(config.getApiKey())) {
                    throw new BusinessException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "AI api key is not configured");
                }

                chatModel = OpenAiChatModel.builder()
                        .apiKey(config.getApiKey())
                        .baseUrl(config.getBaseUrl())
                        .modelName(config.getModelName())
                        .temperature(config.getTemperature())
                        .timeout(config.getTimeout())
                        .logRequests(config.getLogRequests())
                        .logResponses(config.getLogResponses())
                        .build();
            }
            return chatModel;
        }
    }

    public String getModelName() {
        var modelName = aiProperties.getChatModel().getModelName();
        return StringUtils.hasText(modelName) ? modelName : "unknown";
    }
}
