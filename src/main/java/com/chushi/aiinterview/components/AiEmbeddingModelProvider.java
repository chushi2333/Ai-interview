package com.chushi.aiinterview.components;

import com.chushi.aiinterview.configurations.AiProperties;
import com.chushi.aiinterview.exceptions.BusinessException;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiEmbeddingModelProvider {
    @Resource
    private AiProperties aiProperties;

    private volatile EmbeddingModel embeddingModel;

    public EmbeddingModel getEmbeddingModel() {
        // Embedding 模型是远程客户端，第一次真正使用时再创建，避免应用启动阶段就强依赖 key 和网络。
        var configuredEmbeddingModel = embeddingModel;
        if (configuredEmbeddingModel != null) {
            return configuredEmbeddingModel;
        }

        synchronized (this) {
            if (embeddingModel == null) {
                var config = aiProperties.getEmbeddingModel();
                if (!StringUtils.hasText(config.getApiKey())) {
                    throw new BusinessException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "AI embedding api key is not configured");
                }

                // 这里继续使用 OpenAI-compatible 客户端，后续只需要通过 baseUrl/modelName 切换供应商。
                embeddingModel = OpenAiEmbeddingModel.builder()
                        .apiKey(config.getApiKey())
                        .baseUrl(config.getBaseUrl())
                        .modelName(config.getModelName())
                        .dimensions(config.getDimension())
                        .timeout(config.getTimeout())
                        .logRequests(config.getLogRequests())
                        .logResponses(config.getLogResponses())
                        .build();
            }
            return embeddingModel;
        }
    }

    public float[] embed(String text) {
        // 索引和检索都会走这里：题目文本、用户 query 都先转成同一种向量空间里的 float 数组。
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "embedding text must not be empty");
        }

        var embedding = getEmbeddingModel().embed(text).content();
        var vector = embedding.vector();
        // pgvector 的 vector(n) 维度是建表时固定的，所以模型实际返回维度必须提前校验。
        assertConfiguredDimension(vector);
        return vector;
    }

    public String embedAsPgVector(String text) {
        // Mapper 写入时会使用 #{embedding}::vector，这里提前转成 pgvector 接受的 [1,2,3] 字面量。
        return toPgVectorLiteral(embed(text));
    }

    public String getModelName() {
        var modelName = aiProperties.getEmbeddingModel().getModelName();
        return StringUtils.hasText(modelName) ? modelName : "unknown";
    }

    public Integer getConfiguredDimension() {
        return aiProperties.getEmbeddingModel().getDimension();
    }

    void assertConfiguredDimension(float[] vector) {
        // 这里把“模型选错 / 维度配置错”的问题提前暴露，避免写数据库时才出现 vector 维度错误。
        var configuredDimension = getConfiguredDimension();
        if (configuredDimension == null || configuredDimension <= 0) {
            throw new BusinessException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "AI embedding dimension is not configured");
        }
        if (vector == null || vector.length != configuredDimension) {
            var actualDimension = vector == null ? 0 : vector.length;
            throw new BusinessException(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "AI embedding dimension mismatch, configured=" + configuredDimension + ", actual=" + actualDimension
            );
        }
    }

    public static String toPgVectorLiteral(float[] vector) {
        // pgvector 可以接收文本形式的向量，例如 [0.1,-0.2,0.3]，比在 Java 侧引入专用类型更轻量。
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("embedding vector must not be empty");
        }

        var builder = new StringBuilder(vector.length * 8);
        builder.append('[');
        for (int i = 0; i < vector.length; i++) {
            var value = vector[i];
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("embedding vector contains non-finite value at index " + i);
            }
            if (i > 0) {
                builder.append(',');
            }
            builder.append(Float.toString(value));
        }
        builder.append(']');
        return builder.toString();
    }
}
