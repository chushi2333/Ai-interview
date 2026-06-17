package com.chushi.aiinterview.schedulers;

import com.chushi.aiinterview.configurations.RagProperties;
import com.chushi.aiinterview.services.AiRagIndexService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "rag.index-schedule", name = "enabled", havingValue = "true")
public class AiRagIndexScheduler {
    @Resource
    private AiRagIndexService aiRagIndexService;

    @Resource
    private RagProperties ragProperties;

    @Scheduled(cron = "${rag.index-schedule.cron:0 0 3 * * *}", zone = "${rag.index-schedule.zone:Asia/Shanghai}")
    public void rebuildRecentQuestionIndexes() {
        var limit = normalizeLimit(ragProperties.getIndexSchedule().getLimit());
        try {
            // 定时任务只做“最近题目补索引”，真正的 chunk 切分和 embedding 写入仍复用批量索引服务。
            var result = aiRagIndexService.rebuildQuestionIndexBatch(null, limit);
            log.info(
                    "AiRagIndexScheduleFinished: requested={}, success={}, failed={}",
                    result.getRequestedCount(),
                    result.getSuccessCount(),
                    result.getFailedCount()
            );
        } catch (Exception e) {
            // 定时索引是后台维护任务，失败只能记录日志，不能影响应用主流程。
            log.warn("AiRagIndexScheduleException: {}", e.getMessage(), e);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 20;
        }
        return Math.min(Math.max(limit, 1), 50);
    }
}
