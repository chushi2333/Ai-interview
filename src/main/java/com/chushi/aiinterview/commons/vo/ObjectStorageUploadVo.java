package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ObjectStorageUploadVo {
    // 对象所在 bucket，方便前端区分资源用途
    private String bucket;

    // 对象 key，可直接回填到正文内容里
    private String objectKey;

    // 对象访问地址，富文本编辑器可直接使用
    private String url;
}
