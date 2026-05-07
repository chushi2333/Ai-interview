CREATE TABLE `question_practice_record`
(
    `id`               BIGINT   NOT NULL COMMENT '刷题记录ID',
    `user_id`          BIGINT   NOT NULL COMMENT '用户ID',
    `question_id`      BIGINT   NOT NULL COMMENT '题目ID',
    `is_correct`       TINYINT  NOT NULL COMMENT '是否答对：0否 1是',
    `duration_seconds` INT      NULL COMMENT '作答耗时，单位秒',
    `practice_date`    DATE     NOT NULL COMMENT '刷题日期',
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='刷题记录表'
  ROW_FORMAT = Dynamic;

CREATE INDEX `idx_qpr_user_practice_date`
    ON `question_practice_record` (`user_id`, `practice_date`);

CREATE INDEX `idx_qpr_user_id`
    ON `question_practice_record` (`user_id`, `id`);

CREATE INDEX `idx_qpr_question_id`
    ON `question_practice_record` (`question_id`);
