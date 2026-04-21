package com.chushi.aiinterview.services;

import java.util.List;

public interface SeaweedFsService {
    String upload(String bucketName, String originalFilename, byte[] content, String contentType);

    void delete(String bucketName, String filename);

    void deleteBatch(String bucketName, List<String> filenames);
}
