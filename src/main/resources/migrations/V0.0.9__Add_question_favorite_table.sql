CREATE TABLE question_favorite
(
    id          BIGINT PRIMARY KEY COMMENT '主键ID',
    user_id     BIGINT      NOT NULL COMMENT '用户ID',
    question_id BIGINT      NOT NULL COMMENT '题目ID',
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    CONSTRAINT fk_question_favorite_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_question_favorite_question FOREIGN KEY (question_id) REFERENCES question (id),
    CONSTRAINT uq_question_favorite_user_question UNIQUE (user_id, question_id)
) COMMENT ='题目收藏记录表';

CREATE INDEX idx_question_favorite_user_id ON question_favorite (user_id);
CREATE INDEX idx_question_favorite_question_id ON question_favorite (question_id);
