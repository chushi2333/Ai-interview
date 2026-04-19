CREATE TABLE `users`
(
    `id`        bigint                                                        NOT NULL COMMENT '用户 ID',
    `phone`     varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '用户手机号',
    `password`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '用户密码',
    `email`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL     DEFAULT NULL COMMENT '用户邮箱',
    `roles`     bigint                                                        NOT NULL DEFAULT 1 COMMENT '用户角色',
    `join_time` datetime                                                      NOT NULL COMMENT '用户注册时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `index_phone` (`phone` ASC) USING BTREE,
    UNIQUE INDEX `index_email` (`email` ASC) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '用户信息表'
  ROW_FORMAT = Dynamic;
