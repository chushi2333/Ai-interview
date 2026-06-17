package com.chushi.aiinterview.configurations;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SeaweedFsProperties.class)
@ConditionalOnProperty(prefix = "seaweedfs", name = "enabled", havingValue = "true")
public class SeaweedFsConfiguration {
    private final SeaweedFsProperties seaweedFsProperties;

    @Bean
    public S3Client seaweedFsS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(seaweedFsProperties.getEndpoint()))
                .region(Region.AP_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        seaweedFsProperties.getAccessKey(),
                                        seaweedFsProperties.getSecretKey()
                                )
                        )
                )
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .build();
    }
}
