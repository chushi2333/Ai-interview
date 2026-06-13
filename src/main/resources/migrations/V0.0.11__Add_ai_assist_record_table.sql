CREATE TABLE `ai_assist_record`
(
    `id`            BIGINT        NOT NULL COMMENT 'AI助教调用记录ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `question_id`   BIGINT        NOT NULL COMMENT '题目ID',
    `assist_type`   VARCHAR(64)   NOT NULL COMMENT '助教类型',
    `user_input`    TEXT          NULL COMMENT '用户补充输入',
    `content`       MEDIUMTEXT    NULL COMMENT 'AI返回内容',
    `model_name`    VARCHAR(128)  NOT NULL COMMENT '模型名称',
    `status`        VARCHAR(32)   NOT NULL COMMENT '调用状态：success成功 failed失败',
    `error_message` VARCHAR(1024) NULL COMMENT '失败错误信息',
    `latency_ms`    BIGINT        NULL COMMENT '调用耗时，单位毫秒',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='AI助教调用记录表'
  ROW_FORMAT = Dynamic;

CREATE INDEX `idx_aar_user_question_id`
    ON `ai_assist_record` (`user_id`, `question_id`, `id`);

CREATE INDEX `idx_aar_user_id`
    ON `ai_assist_record` (`user_id`, `id`);

CREATE INDEX `idx_aar_question_id`
    ON `ai_assist_record` (`question_id`);
