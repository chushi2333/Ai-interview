CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_rag_chunk
(
    id          BIGINT       NOT NULL,
    question_id BIGINT       NOT NULL,
    chunk_index INT          NOT NULL,
    source_type VARCHAR(32)  NOT NULL,
    title       VARCHAR(255) NULL,
    content     TEXT         NOT NULL,
    embedding   vector(1536) NOT NULL,
    metadata    JSONB        NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_ai_rag_chunk_question_id
    ON ai_rag_chunk (question_id);

CREATE INDEX IF NOT EXISTS idx_ai_rag_chunk_embedding
    ON ai_rag_chunk USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
