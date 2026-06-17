# AI V4 RAG 需求分析

## 1. V4 做什么

V4 要做 RAG，也就是检索增强生成。

目标是：AI 回答用户问题前，先从资料库里检索相关内容，再把检索结果放进 Prompt，让模型基于资料回答。

完整目标链路：

```text
用户问题
-> 生成 query embedding
-> PostgreSQL pgvector 相似度检索
-> 取 topK 相关资料片段
-> 放进聊天 Prompt
-> 模型回答
```

## 2. V4 不做什么

V4 第一版不做以下内容：

- 不检索用户长期记忆。
- 不检索所有历史 AI 对话。
- 不做自动全量题库索引任务。
- 不做复杂文档管理后台。
- 不做多向量库适配。
- 不做 Agent 工具调用。

原因：

- V3 已经把用户长期记忆放进 Prompt，暂时不需要再用 RAG 检索用户记忆。
- 第一版 RAG 的核心是先跑通 pgvector 的索引、检索、调试、Prompt 接入。
- 范围过大时，很难判断问题出在 embedding、chunk、向量库、检索策略还是 Prompt。

## 3. 技术选型

使用：

```text
PostgreSQL + pgvector
```

不使用：

```text
MySQL 存向量
Elasticsearch dense_vector
专门向量数据库
```

选择 pgvector 的原因：

- 用户以前使用过 pgvector，学习成本低。
- PostgreSQL 原生表结构适合保存 chunk 元数据。
- pgvector 支持向量相似度检索，第一版足够用。
- 后续可以用索引优化，例如 `ivfflat` 或 `hnsw`。

## 4. V4.1 第一版范围

第一版只做：

```text
题目知识 RAG
```

也就是把题目相关内容切成 chunk 后写入 pgvector。

第一版数据来源：

- 题目标题。
- 题目内容。
- 参考答案。
- 题目标签。
- 题目难度。
- 所属题库名称。

第一版不做用户记忆 RAG，因为用户记忆已经在 V3 中作为完整摘要进入 Prompt。

## 5. 数据流

### 5.1 索引流程

```text
选择一道题 questionId
-> 读取题目详情
-> 组装可索引文本
-> 切分 chunk
-> 调 embedding 模型
-> 写入 PostgreSQL ai_rag_chunk
```

### 5.2 检索流程

```text
用户输入 query
-> 调 embedding 模型生成 query embedding
-> pgvector 相似度检索 topK
-> 返回命中的 chunk
```

### 5.3 聊天接入流程

```text
用户发送 AI 对话消息
-> 用当前用户问题做 RAG 检索
-> 获取相关资料片段
-> 放进 Prompt 的 RAG 区域
-> 调聊天模型回答
```

## 6. PostgreSQL 表结构草案

### 6.1 ai_rag_chunk

```sql
CREATE TABLE ai_rag_chunk (
    id BIGINT PRIMARY KEY,
    source_type VARCHAR(64) NOT NULL,
    source_id BIGINT NOT NULL,
    question_id BIGINT NULL,
    title VARCHAR(255) NULL,
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    token_count INT NULL,
    chunk_index INT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

字段含义：

- `source_type`：来源类型。第一版可以是 `question`。
- `source_id`：来源 ID。第一版等于 `question_id`。
- `question_id`：题目 ID，方便按题目过滤。
- `title`：chunk 标题，方便调试展示。
- `content`：chunk 原文。
- `embedding`：向量。
- `token_count`：估算 token 数，方便后续控制 Prompt 长度。
- `chunk_index`：同一来源下的 chunk 序号。

### 6.2 向量维度

第一版默认：

```text
vector(1536)
```

这个维度需要和 embedding 模型一致。

如果后续 embedding 模型维度不是 1536，需要同步修改：

- 表结构。
- 配置项。
- 写入校验。

## 7. Docker 环境需求

需要在 `docker-compose.dev.yml` 里新增 PostgreSQL + pgvector 容器。

建议镜像：

```text
pgvector/pgvector:pg16
```

建议服务名：

```text
ai-interview-postgres
```

建议端口：

```text
5432:5432
```

建议数据库：

```text
ai_interview_rag
```

说明：

- 主业务库暂时仍然使用 MySQL。
- RAG 向量库单独使用 PostgreSQL。
- 第一版不迁移现有业务数据到 PostgreSQL。

## 8. 配置项需求

新增 RAG 数据源配置：

```yaml
rag:
  datasource:
    url: jdbc:postgresql://localhost:5433/ai_interview_rag
    username: postgres
    password: postgres
```

新增 embedding 模型配置：

```yaml
ai:
  embedding-model:
    api-key: ${AI_EMBEDDING_API_KEY:${AI_API_KEY:${OPENAI_API_KEY:}}}
    base-url: ${AI_EMBEDDING_BASE_URL:${AI_BASE_URL:https://api.openai.com/v1}}
    model-name: ${AI_EMBEDDING_MODEL:text-embedding-3-small}
    dimension: ${AI_EMBEDDING_DIMENSION:1536}
    timeout: ${AI_EMBEDDING_TIMEOUT:60s}
```

说明：

- embedding 模型可以和聊天模型使用同一个 key。
- 也可以单独配置 embedding key、baseUrl、modelName。
- 维度必须和 pgvector 表一致。

## 9. 接口设计草案

### 9.1 手动索引题目

```http
POST /api/ai/rag/questions/{questionId}/index
```

作用：

- 读取指定题目。
- 切分 chunk。
- 生成 embedding。
- 写入 pgvector。

权限：

- 第一版建议只允许 `ADMIN`、`SUPER_ADMIN`。

返回示例：

```json
{
  "questionId": 1,
  "chunkCount": 3,
  "indexed": true
}
```

### 9.2 调试检索

```http
GET /api/ai/rag/search?query=HashMap扩容&question_id=1&top_k=5
```

作用：

- 对 query 生成 embedding。
- 检索相关 chunk。
- 返回命中结果和相似度。

返回示例：

```json
{
  "query": "HashMap扩容",
  "topK": 5,
  "results": [
    {
      "chunkId": 100,
      "questionId": 1,
      "title": "HashMap 原理",
      "content": "...",
      "score": 0.82
    }
  ]
}
```

### 9.3 查询题目索引状态

```http
GET /api/ai/rag/questions/{questionId}/index-status
```

作用：

- 查看某道题是否已经索引。
- 查看 chunk 数量。
- 查看最近更新时间。

## 10. Prompt 接入位置

V4 接入后，聊天 Prompt 结构变成：

```text
当前题目上下文
用户长期学习记忆
当前会话长期摘要
最近对话历史
RAG 检索资料
当前用户问题
输出要求
```

RAG 区域示例：

```text
# RAG 检索资料
1. 来源：HashMap 原理
内容：...
2. 来源：HashMap 面试题答案
内容：...
```

如果没有命中资料：

```text
# RAG 检索资料
无
```

## 11. Chunk 切分策略

第一版先使用简单规则，不引入复杂文本切分器。

建议规则：

- 每个 chunk 最多 800 字符。
- 相邻 chunk 可以有 100 字符 overlap。
- 题目标题、难度、标签、题库名作为上下文前缀。
- 题目内容和参考答案可以分别切分。

chunk 内容示例：

```text
题目：HashMap 原理
题库：Java 基础
难度：中等
标签：Java, 集合, HashMap
内容：...
```

## 12. 成本控制

RAG 会增加 embedding 调用。

成本主要来自：

- 索引时：每个 chunk 调一次 embedding。
- 检索时：每次用户 query 调一次 embedding。

第一版成本控制：

- 只手动索引指定题目。
- 不自动全量索引所有题目。
- 不每次保存题目都自动重建索引。
- 聊天检索 topK 默认不超过 5。
- RAG 资料进入 Prompt 前限制总长度。

## 13. 风险点

### 13.1 维度不匹配

embedding 模型维度必须和 `vector(n)` 一致。

否则写入会失败。

### 13.2 资料过长

检索结果不能无限放入 Prompt。

需要限制：

- topK。
- 单条 chunk 长度。
- RAG 区域总长度。

### 13.3 旧索引失效

题目内容更新后，旧 chunk 可能过期。

第一版可以手动重新索引：

```http
POST /api/ai/rag/questions/{questionId}/index
```

后续再做自动重建。

### 13.4 PostgreSQL 和 MySQL 双数据库复杂度

主业务数据在 MySQL，向量数据在 PostgreSQL。

需要注意：

- 两套数据源配置。
- 两套事务边界。
- 题目删除或更新时的索引一致性。

第一版先不做强事务一致性。

## 14. 开发步骤

### V4.1 环境和依赖

- Docker 增加 PostgreSQL + pgvector。
- Spring Boot 增加 PostgreSQL JDBC 依赖。
- 增加 RAG 数据源配置。

### V4.2 表结构和 Mapper

- 创建 `ai_rag_chunk` 表。
- 创建实体和 Mapper。
- 支持 insert、delete by questionId、search topK。

### V4.3 Embedding 模型封装

- 新增 `AiEmbeddingModelProvider`。
- 支持配置 baseUrl、apiKey、modelName、dimension。
- 提供 `embed(String text)` 方法。

### V4.4 手动索引题目接口

- `POST /api/ai/rag/questions/{questionId}/index`。
- 读取题目详情。
- chunk 切分。
- 生成 embedding。
- 写入 pgvector。

### V4.5 检索调试接口

- `GET /api/ai/rag/search`。
- 返回 query、topK、命中 chunk、score。

### V4.6 聊天 Prompt 接入 RAG

- 在 `sendMessage` 中先检索 RAG。
- 把命中资料放进 Prompt。
- 文档记录 Prompt 结构变化。

### V4.7 真实联调验证

- 索引一道题。
- 用相关问题检索。
- 确认命中 chunk。
- 发送 AI 对话，确认回答参考了 RAG 资料。

## 15. 第一版验收标准

V4 第一版完成时，需要满足：

1. PostgreSQL + pgvector 能通过 Docker 启动。
2. `ai_rag_chunk` 表能保存向量。
3. 可以手动索引一道题。
4. 可以用 query 检索命中 chunk。
5. 检索接口能返回 score 和来源。
6. 聊天 Prompt 能带上 RAG 检索资料。
7. 文档记录每一步做了什么、为什么、怎么验证。
