package com.chushi.aiinterview.controller;

import com.chushi.aiinterview.annotations.CurrentUser;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.enums.UserRole;
import com.chushi.aiinterview.commons.vo.QuestionBankVo;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.commons.vo.ObjectStorageUploadVo;
import com.chushi.aiinterview.configurations.SeaweedFsProperties;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.exceptions.BusinessException;
import com.chushi.aiinterview.services.QuestionBankService;
import com.chushi.aiinterview.services.SeaweedFsService;
import com.chushi.aiinterview.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;

@RestController
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "seaweedfs", name = "enabled", havingValue = "true")
public class ObjectStorageController extends BaseController {
    private static final String USER_AVATARS_BUCKET = "user-avatars";
    private static final String QUESTION_BANK_PICTURES_BUCKET = "question-bank-pictures";
    private static final String QUESTION_CONTENT_IMAGES_BUCKET = "question-content-images";

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final long MAX_QUESTION_BANK_PICTURE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_QUESTION_CONTENT_IMAGE_SIZE = 5 * 1024 * 1024;

    private final SeaweedFsService seaweedFsService;
    private final UserService userService;
    private final QuestionBankService questionBankService;
    private final SeaweedFsProperties seaweedFsProperties;

    @PostMapping(path = "/api/user/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传用户头像")
    @RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<User> uploadUserAvatar(@Parameter(description = "图片文件")
            @RequestParam("image") MultipartFile imageFile,
            @CurrentUser User currentUser
    ) throws IOException {

        checkImage(imageFile, MAX_AVATAR_SIZE);
        var oldAvatar = currentUser.getAvatar();
        String newAvatar = null;

        try {
            newAvatar = seaweedFsService.upload(
                    USER_AVATARS_BUCKET,
                    imageFile.getOriginalFilename(),
                    imageFile.getBytes(),
                    imageFile.getContentType()
            );
            var user = userService.updateAvatar(currentUser.getId(), newAvatar);
            // 头像更新成功后再删旧文件，避免用户头像被误删
            deleteOldFile(USER_AVATARS_BUCKET, oldAvatar, newAvatar);
            return wrap(user);
        } catch (Exception e) {
            rollbackUploadedFile(USER_AVATARS_BUCKET, newAvatar);
            throw e;
        }
    }

    @PostMapping(path = "/api/question-bank/{questionBankId}/cover/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传题库封面")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<QuestionBankVo> uploadQuestionBankCover(
            @PathVariable Long questionBankId,
            @Parameter(description = "图片文件")
            @RequestParam("image") MultipartFile imageFile
    ) throws IOException {
        checkImage(imageFile, MAX_QUESTION_BANK_PICTURE_SIZE);
        var oldPicture = questionBankService.getQuestionBankById(questionBankId).getPicture();
        String newPicture = null;

        try {
            newPicture = seaweedFsService.upload(
                    QUESTION_BANK_PICTURES_BUCKET,
                    imageFile.getOriginalFilename(),
                    imageFile.getBytes(),
                    imageFile.getContentType()
            );
            var questionBank = questionBankService.updateQuestionBankPicture(questionBankId, newPicture);
            // 封面更新成功后清理旧文件，避免对象存储里残留废弃图片
            deleteOldFile(QUESTION_BANK_PICTURES_BUCKET, oldPicture, newPicture);
            return wrap(QuestionBankVo.builder()
                    .id(questionBank.getId())
                    .title(questionBank.getTitle())
                    .description(questionBank.getDescription())
                    .picture(questionBank.getPicture())
                    .editTime(questionBank.getEditTime())
                    .createTime(questionBank.getCreateTime())
                    .updateTime(questionBank.getUpdateTime())
                    .build());
        } catch (Exception e) {
            rollbackUploadedFile(QUESTION_BANK_PICTURES_BUCKET, newPicture);
            throw e;
        }
    }

    @PostMapping(path = "/api/question/content-image/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传题目正文或题解图片")
    @RequireRole(value = {UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
    public Response<ObjectStorageUploadVo> uploadQuestionContentImage(
            @Parameter(description = "图片文件")
            @RequestParam("image") MultipartFile imageFile
    ) throws IOException {
        checkImage(imageFile, MAX_QUESTION_CONTENT_IMAGE_SIZE);
        var objectKey = seaweedFsService.upload(
                QUESTION_CONTENT_IMAGES_BUCKET,
                imageFile.getOriginalFilename(),
                imageFile.getBytes(),
                imageFile.getContentType()
        );
        return wrap(ObjectStorageUploadVo.builder()
                .bucket(QUESTION_CONTENT_IMAGES_BUCKET)
                .objectKey(objectKey)
                .url(buildObjectUrl(QUESTION_CONTENT_IMAGES_BUCKET, objectKey))
                .build());
    }

    // 校验图片文件合法性，只允许上传可读取的图片
    void checkImage(MultipartFile imageFile, long maxSize) throws IOException {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "Uploads cannot be empty");
        }

        var contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "Uploads must be images");
        }

        if (imageFile.getSize() > maxSize) {
            throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "Uploads exceed size limit");
        }

        if (ImageIO.read(imageFile.getInputStream()) == null) {
            throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "The image cannot be read");
        }
    }

    // 上传后的数据库更新失败时，回滚刚写入对象存储的新文件
    void rollbackUploadedFile(String bucketName, String filename) {
        if (!StringUtils.hasText(filename)) {
            return;
        }

        try {
            seaweedFsService.delete(bucketName, filename);
        } catch (Exception e) {
            log.warn("RollbackUploadedFileException: {}", e.getMessage(), e);
        }
    }

    // 新文件替换成功后，删除旧文件避免残留无用图片
    void deleteOldFile(String bucketName, String oldFilename, String newFilename) {
        if (!StringUtils.hasText(oldFilename) || oldFilename.equals(newFilename)) {
            return;
        }

        try {
            seaweedFsService.delete(bucketName, oldFilename);
        } catch (Exception e) {
            log.warn("DeleteOldFileException: {}", e.getMessage(), e);
        }
    }

    // 正文和题解里只保存对象存储地址，不直接存二进制内容
    String buildObjectUrl(String bucketName, String filename) {
        var endpoint = seaweedFsProperties.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new BusinessException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "SeaweedFS endpoint is not configured");
        }
        var normalizedEndpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return normalizedEndpoint + "/" + bucketName + "/" + filename;
    }
}
