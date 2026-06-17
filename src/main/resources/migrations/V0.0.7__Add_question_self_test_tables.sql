CREATE TABLE `question_self_test`
(
    `id`          BIGINT        NOT NULL COMMENT '自测题ID',
    `question_id` BIGINT        NOT NULL COMMENT '关联题目ID',
    `content`     VARCHAR(1024) NOT NULL COMMENT '自测题题干',
    `explanation` TEXT          NULL COMMENT '题目解析',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='题目自测题表'
  ROW_FORMAT = Dynamic;

CREATE INDEX `idx_qst_question_id`
    ON `question_self_test` (`question_id`);

CREATE TABLE `question_self_test_option`
(
    `id`           BIGINT       NOT NULL COMMENT '自测题选项ID',
    `self_test_id` BIGINT       NOT NULL COMMENT '关联自测题ID',
    `option_key`   VARCHAR(8)   NOT NULL COMMENT '选项标识，如 A/B/C/D',
    `content`      VARCHAR(512) NOT NULL COMMENT '选项内容',
    `is_correct`   TINYINT      NOT NULL COMMENT '是否正确选项：0否 1是',
    `sort_order`   INT          NOT NULL DEFAULT 0 COMMENT '选项排序',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='题目自测题选项表'
  ROW_FORMAT = Dynamic;

CREATE INDEX `idx_qsto_self_test_id`
    ON `question_self_test_option` (`self_test_id`);

CREATE TABLE `question_self_test_record`
(
    `id`               BIGINT   NOT NULL COMMENT '自测记录ID',
    `self_test_id`     BIGINT   NOT NULL COMMENT '关联自测题ID',
    `user_id`          BIGINT   NOT NULL COMMENT '用户ID',
    `selected_answers` VARCHAR(256) NOT NULL COMMENT '用户选择答案，JSON数组字符串',
    `is_correct`       TINYINT  NOT NULL COMMENT '是否答对：0否 1是',
    `duration_seconds` INT      NULL COMMENT '作答耗时，单位秒',
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='题目自测记录表'
  ROW_FORMAT = Dynamic;

CREATE INDEX `idx_qstr_user_id`
    ON `question_self_test_record` (`user_id`);

CREATE INDEX `idx_qstr_self_test_id`
    ON `question_self_test_record` (`self_test_id`);
