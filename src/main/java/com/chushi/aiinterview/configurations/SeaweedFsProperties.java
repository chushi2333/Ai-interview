package com.chushi.aiinterview.configurations;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "seaweedfs")
public class SeaweedFsProperties {
    private boolean enabled = true;

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private List<String> buckets;
}
