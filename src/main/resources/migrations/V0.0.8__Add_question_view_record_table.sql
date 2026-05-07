CREATE TABLE `question_view_record`
(
    `id`          BIGINT   NOT NULL COMMENT '看题记录ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `question_id` BIGINT   NOT NULL COMMENT '题目ID',
    `view_date`   DATE     NOT NULL COMMENT '查看日期',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='看题记录表'
  ROW_FORMAT = Dynamic;

CREATE UNIQUE INDEX `uq_qvr_user_question_date`
    ON `question_view_record` (`user_id`, `question_id`, `view_date`);

CREATE INDEX `idx_qvr_user_view_date`
    ON `question_view_record` (`user_id`, `view_date`);

CREATE INDEX `idx_qvr_user_id`
    ON `question_view_record` (`user_id`, `id`);

CREATE INDEX `idx_qvr_question_id`
    ON `question_view_record` (`question_id`);
