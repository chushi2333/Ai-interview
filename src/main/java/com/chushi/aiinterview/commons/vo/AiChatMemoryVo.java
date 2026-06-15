package com.chushi.aiinterview.commons.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiChatMemoryVo {
    private Long sessionId;

    private String memorySummary;

    private Long summaryMessageId;

    private Integer successMessageCount;

    private Integer recentMessageCount;

    private Integer pendingSummaryMessageCount;

    private String summaryStrategy;

    private Boolean summaryTriggerReady;

    private String summaryTriggerReason;

    private Integer summaryTriggerSuccessMessageCount;

    private Integer summaryRecentMessageReserved;

    private Integer summaryMinSourceMessageCount;
}

