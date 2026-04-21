package com.chushi.aiinterview.components;

import com.chushi.aiinterview.configurations.SeaweedFsProperties;
import com.chushi.aiinterview.exceptions.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "seaweedfs", name = "enabled", havingValue = "true")
public class BucketsInitializer implements CommandLineRunner {
    @Resource
    private S3Client seaweedFsS3Client;

    @Resource
    private SeaweedFsProperties seaweedFsProperties;

    @Override
    public void run(String... args) {
        var bucketNames = seaweedFsProperties.getBuckets();
        if (bucketNames == null || bucketNames.isEmpty()) {
            log.warn("Bucket initialization skipped: no bucket configured");
            return;
        }

        for (var bucketName : bucketNames) {
            checkAndCreateBucket(bucketName);
        }
    }

    private void checkAndCreateBucket(String bucketName) {
        try {
            seaweedFsS3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.debug("Bucket already exists: bucketName={}", bucketName);
        } catch (NoSuchBucketException e) {
            log.info("Bucket not found, creating: bucketName={}", bucketName);
            createBucket(bucketName);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.info("Bucket not found (404), creating: bucketName={}", bucketName);
                createBucket(bucketName);
            } else {
                log.error("Failed to check bucket existence: bucketName={}", bucketName, e);
                throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Bucket initialization failed");
            }
        }
    }

    private void createBucket(String bucketName) {
        try {
            seaweedFsS3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            log.info("Bucket created successfully: bucketName={}", bucketName);
        } catch (Exception e) {
            log.error("Bucket creation failed: bucketName={}", bucketName, e);
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create bucket");
        }
    }
}
