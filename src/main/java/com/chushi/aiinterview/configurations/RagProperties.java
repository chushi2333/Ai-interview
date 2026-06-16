package com.chushi.aiinterview.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    private DatasourceProperties datasource = new DatasourceProperties();

    private ChunkProperties chunk = new ChunkProperties();

    private SearchProperties search = new SearchProperties();

    private DecisionProperties decision = new DecisionProperties();

    @Data
    public static class DatasourceProperties {
        private String url;

        private String username;

        private String password;

        private String driverClassName;
    }

    @Data
    public static class ChunkProperties {
        private Integer maxLength = 1200;

        private Integer overlapLength = 150;
    }

    @Data
    public static class SearchProperties {
        private Double maxDistance = 0.45;
    }

    @Data
    public static class DecisionProperties {
        private Integer minMessageLength = 18;

        private List<String> keywords = List.of(
                "为什么", "怎么", "如何", "是什么", "原理", "底层", "源码",
                "区别", "对比", "流程", "解释", "举例", "复杂度", "场景", "优化"
        );
    }
}
