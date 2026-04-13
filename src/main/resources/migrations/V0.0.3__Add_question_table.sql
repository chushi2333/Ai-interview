CREATE TABLE `question`
(
    `id`          BIGINT        NOT NULL COMMENT '题目ID',
    `title`       VARCHAR(256)  NOT NULL COMMENT '题目标题',
    `content`     TEXT          NOT NULL COMMENT '题目内容',
    `tags`        VARCHAR(1024) NULL COMMENT '题目标签',
    `answer`      TEXT          NULL COMMENT '题目答案',
    `difficulty`  TINYINT       NOT NULL DEFAULT 1 COMMENT '难度：1简单 2中等 3困难',
    `is_member_only` TINYINT    NOT NULL DEFAULT 0 COMMENT '是否会员可见',
    `user_id`     BIGINT        NOT NULL COMMENT '创建用户ID',
    `edit_time`   DATETIME      NOT NULL COMMENT '编辑时间',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='题目表'
  ROW_FORMAT = Dynamic;

CREATE INDEX `idx_question_user_id`
    ON `question` (`user_id`);

CREATE INDEX `idx_question_title`
    ON `question` (`title`);
