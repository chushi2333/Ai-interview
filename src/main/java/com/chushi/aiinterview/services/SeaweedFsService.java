package com.chushi.aiinterview.services;

import com.chushi.aiinterview.commons.vo.ObjectStorageFileVo;

import java.util.List;

public interface SeaweedFsService {
    String upload(String bucketName, String originalFilename, byte[] content, String contentType);

    ObjectStorageFileVo download(String bucketName, String filename);

    void delete(String bucketName, String filename);

    void deleteBatch(String bucketName, List<String> filenames);
}
