ALTER TABLE `ai_chat_session`
    ADD COLUMN `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0未删除 1已删除' AFTER `status`;

CREATE INDEX `idx_acs_user_question_delete_id`
    ON `ai_chat_session` (`user_id`, `question_id`, `is_delete`, `id`);
