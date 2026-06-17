package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiChatSessionVo {
    private Long id;

    private Long questionId;

    private String questionTitle;

    private String title;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
