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
public class AiUserMemoryVo {
    private Boolean hasMemory;

    private String memorySummary;

    private Integer sourceSessionCount;

    private Long lastSourceSessionId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean promptEnabled;

    private String updateStrategy;

    private Integer maxMemoryLength;
}
