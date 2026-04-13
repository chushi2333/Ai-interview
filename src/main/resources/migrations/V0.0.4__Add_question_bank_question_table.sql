CREATE TABLE `question_bank_question`
(
    `id`               BIGINT   NOT NULL COMMENT '关系ID',
    `question_bank_id` BIGINT   NOT NULL COMMENT '题库ID',
    `question_id`      BIGINT   NOT NULL COMMENT '题目ID',
    `user_id`          BIGINT   NOT NULL COMMENT '创建用户ID',
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='题库题目关联表'
  ROW_FORMAT = Dynamic;

CREATE UNIQUE INDEX `uk_question_bank_id_question_id`
    ON `question_bank_question` (`question_bank_id`, `question_id`);

CREATE INDEX `idx_qbq_question_bank_id`
    ON `question_bank_question` (`question_bank_id`);

CREATE INDEX `idx_qbq_question_id`
    ON `question_bank_question` (`question_id`);

CREATE INDEX `idx_qbq_user_id`
    ON `question_bank_question` (`user_id`);
