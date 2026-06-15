CREATE TABLE `ai_user_memory`
(
    `id`                     BIGINT      NOT NULL COMMENT 'AI用户长期记忆ID',
    `user_id`                BIGINT      NOT NULL COMMENT '用户ID',
    `memory_summary`         MEDIUMTEXT  NULL COMMENT '用户级长期学习记忆摘要',
    `source_session_count`   INT         NOT NULL DEFAULT 0 COMMENT '已合并进用户记忆的会话数量',
    `last_source_session_id` BIGINT      NULL COMMENT '最近一次合并来源会话ID',
    `create_time`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='AI用户长期记忆表'
  ROW_FORMAT = Dynamic;

CREATE UNIQUE INDEX `uk_aum_user_id`
    ON `ai_user_memory` (`user_id`);
