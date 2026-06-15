CREATE TABLE `ai_chat_session`
(
    `id`          BIGINT       NOT NULL COMMENT 'AI对话会话ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `question_id` BIGINT       NOT NULL COMMENT '题目ID',
    `title`       VARCHAR(128) NULL COMMENT '会话标题',
    `status`      VARCHAR(32)  NOT NULL COMMENT '会话状态：active活跃 archived归档',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='AI对话会话表'
  ROW_FORMAT = Dynamic;

CREATE INDEX `idx_acs_user_question_id`
    ON `ai_chat_session` (`user_id`, `question_id`, `id`);

CREATE INDEX `idx_acs_user_id`
    ON `ai_chat_session` (`user_id`, `id`);

CREATE TABLE `ai_chat_message`
(
    `id`            BIGINT        NOT NULL COMMENT 'AI对话消息ID',
    `session_id`    BIGINT        NOT NULL COMMENT '会话ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `question_id`   BIGINT        NOT NULL COMMENT '题目ID',
    `role`          VARCHAR(32)   NOT NULL COMMENT '消息角色：user assistant system',
    `content`       MEDIUMTEXT    NOT NULL COMMENT '消息内容',
    `model_name`    VARCHAR(128)  NULL COMMENT '模型名称，仅assistant消息需要',
    `status`        VARCHAR(32)   NOT NULL COMMENT '消息状态：success failed',
    `error_message` VARCHAR(1024) NULL COMMENT '失败错误信息',
    `latency_ms`    BIGINT        NULL COMMENT 'AI回复耗时，单位毫秒',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='AI对话消息表'
  ROW_FORMAT = Dynamic;

CREATE INDEX `idx_acm_session_id`
    ON `ai_chat_message` (`session_id`, `id`);

CREATE INDEX `idx_acm_user_question_id`
    ON `ai_chat_message` (`user_id`, `question_id`, `id`);
