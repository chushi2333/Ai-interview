SET @nickname_column_count := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'nickname'
);

SET @add_nickname_sql := IF(
    @nickname_column_count = 0,
    'ALTER TABLE users ADD COLUMN nickname VARCHAR(32) NULL COMMENT ''用户昵称'' AFTER phone',
    'SELECT ''nickname column already exists'''
);

PREPARE add_nickname_stmt FROM @add_nickname_sql;
EXECUTE add_nickname_stmt;
DEALLOCATE PREPARE add_nickname_stmt;
