ALTER TABLE `ai_chat_session`
    ADD COLUMN `memory_summary` MEDIUMTEXT NULL COMMENT 'AI对话长期记忆摘要' AFTER `is_delete`,
    ADD COLUMN `summary_message_id` BIGINT NULL COMMENT '摘要已覆盖到的消息ID' AFTER `memory_summary`;
