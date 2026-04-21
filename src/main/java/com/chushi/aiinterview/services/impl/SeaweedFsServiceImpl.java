package com.chushi.aiinterview.services.impl;

import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.services.SeaweedFsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "seaweedfs", name = "enabled", havingValue = "true")
public class SeaweedFsServiceImpl implements SeaweedFsService {
    private final S3Client seaweedFsS3Client;

    @Override
    public String upload(String bucketName, String originalFilename, byte[] content, String contentType) {
        var suffix = getSuffixFromContentType(contentType);
        var filename = UUID.randomUUID().toString().replace("-", "") + suffix;

        try {
            var request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();

            var response = seaweedFsS3Client.putObject(request, RequestBody.fromBytes(content));
            if (!response.sdkHttpResponse().isSuccessful()) {
                log.error("Image upload failed: HTTP response not successful. bucket={}, key={}", bucketName, filename);
                throw new BusinessException(500, "Image upload failed");
            }

            log.info("Image uploaded successfully. bucket={}, key={}, etag={}", bucketName, filename, response.eTag());
            return filename;
        } catch (S3Exception e) {
            log.error("Image upload failed (S3Exception). bucket={}, key={}, code={}, message={}",
                    bucketName, filename, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);
            throw new BusinessException(500, "Image upload failed");
        } catch (Exception e) {
            log.error("Image upload failed (Unknown Exception). bucket={}, key={}", bucketName, filename, e);
            throw new BusinessException(500, "Image upload failed");
        }
    }

    @Override
    public void delete(String bucketName, String filename) {
        try {
            var request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filename)
                    .build();
            seaweedFsS3Client.deleteObject(request);
            log.info("Image deleted successfully. bucket={}, key={}", bucketName, filename);
        } catch (S3Exception e) {
            log.error("Image delete failed (S3Exception). bucket={}, key={}, code={}, message={}",
                    bucketName, filename, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);
            throw new BusinessException(500, "Image delete failed");
        } catch (Exception e) {
            log.error("Image delete failed (Unknown Exception). bucket={}, key={}", bucketName, filename, e);
            throw new BusinessException(500, "Image delete failed");
        }
    }

    @Override
    public void deleteBatch(String bucketName, List<String> filenames) {
        try {
            var request = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder()
                            .objects(filenames.stream()
                                    .map(filename -> ObjectIdentifier.builder().key(filename).build())
                                    .toList())
                            .build())
                    .build();
            seaweedFsS3Client.deleteObjects(request);
        } catch (S3Exception e) {
            log.error("Image delete failed (S3Exception). bucket={}, keys={}, code={}, message={}",
                    bucketName, filenames, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);
            throw new BusinessException(500, "Image delete failed");
        } catch (Exception e) {
            log.error("Image delete failed (Unknown Exception). bucket={}, keys={}", bucketName, filenames, e);
            throw new BusinessException(500, "Image delete failed");
        }
    }

    private String getSuffixFromContentType(String contentType) {
        if (contentType == null) {
            throw new BusinessException(400, "Content-Type cannot be null");
        }
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            default -> throw new BusinessException(400, "Unsupported image type: " + contentType);
        };
    }
}
