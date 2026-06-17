CREATE TABLE `question_bank`
(
    `id`          BIGINT        NOT NULL COMMENT '题库ID',
    `title`       VARCHAR(256)  NOT NULL COMMENT '题库标题',
    `description` TEXT          NULL COMMENT '题库描述',
    `picture`     VARCHAR(2048) NULL COMMENT '题库封面',
    `user_id`     BIGINT        NOT NULL COMMENT '创建用户ID',
    `edit_time`   DATETIME      NOT NULL COMMENT '编辑时间',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='题库表'
  ROW_FORMAT = Dynamic;

CREATE INDEX `idx_question_bank_user_id`
    ON `question_bank` (`user_id`);

CREATE INDEX `idx_question_bank_title`
    ON `question_bank` (`title`);
