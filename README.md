# AI Interview

一个面向程序员面试刷题场景的学习平台后端项目。

项目以 Spring Boot 为主线，逐步实现题库管理、搜索、学习行为记录、对象存储、缓存、消息队列，以及 AI 面试助教能力。当前重点是把传统后端业务和 AI 应用工程化结合起来，而不是只做一个简单的模型调用 Demo。

## 项目亮点

- 题库与题目管理：支持题目、题库、题库题目关系等核心业务
- 学习行为记录：支持看题记录、单选自测、练习统计和签到记录
- 搜索能力：使用 Elasticsearch 构建题目搜索视图
- 缓存设计：使用 Redis 缓存题目详情和题库详情
- 对象存储：使用 SeaweedFS 保存用户头像、题库封面和题目图片
- 消息队列：使用 RabbitMQ 异步同步题目数据到 Elasticsearch
- AI 助教：基于 LangChain4j 接入 OpenAI-compatible Chat Model
- RAG 检索：基于 PostgreSQL + pgvector 实现题目知识检索增强
- 记忆系统：实现最近上下文、会话摘要和用户长期学习记忆

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 后端 | Java 17, Spring Boot 3, Spring MVC |
| 数据访问 | MyBatis, MySQL, Flyway |
| 缓存 | Redis |
| 消息队列 | RabbitMQ |
| 搜索 | Elasticsearch |
| 对象存储 | SeaweedFS |
| AI | LangChain4j, Chat Model, Embedding Model |
| RAG | PostgreSQL, pgvector |
| 本地环境 | Docker Compose |

## 核心功能

### 题库与题目

- 题目创建、修改、删除、查询
- 题库创建、修改、删除、查询
- 题库与题目的关联管理
- 会员题权限控制
- 题目标签与难度管理

### 搜索与同步

- MySQL 作为主业务数据库
- Elasticsearch 作为搜索视图
- RabbitMQ 负责异步同步题目变更
- 支持关键词搜索、难度过滤、标签过滤和排序
- 配置死信队列，方便排查同步失败消息

### 学习行为

- 用户签到记录
- 题目查看记录
- 单选自测题管理与提交
- 自测结果记录
- 年度学习热力图数据补齐

### 文件存储

- 用户头像上传
- 题库封面上传
- 题目内容图片上传
- 使用 SeaweedFS 提供本地对象存储能力

## AI 模块

当前 AI 模块不是单纯调用一次大模型接口，而是围绕“面试刷题助教”做了分层设计。

### AI 题目追问

用户可以围绕当前题目进行追问，AI 会结合题目标题、内容、参考答案和用户输入进行回答。

### 对话记忆

- 保存用户消息和模型回复
- 最近对话作为短期上下文进入 Prompt
- 控制单条历史和总历史长度，避免 Prompt 过长

### 会话摘要

当会话消息变长后，将较早历史压缩成会话摘要。

目的：

- 减少上下文 token 成本
- 保留关键学习信息
- 避免每次都把完整历史塞进 Prompt

### 用户长期记忆

基于会话摘要沉淀用户级学习画像，例如薄弱点、偏好的解释方式、未解决问题等。

长期记忆会在后续对话中作为个性化上下文使用。

## RAG 检索增强

RAG 模块用于把题库知识接入 AI 回答流程。

### 索引流程

```text
题目数据
  ↓
多段 chunk 切分
  ↓
Embedding 模型向量化
  ↓
写入 PostgreSQL + pgvector
```

### 检索流程

```text
用户问题
  ↓
RAG 决策服务判断是否需要检索
  ↓
Query Embedding
  ↓
pgvector topK 相似度检索
  ↓
distance 阈值过滤
  ↓
检索资料进入 Prompt
  ↓
Chat Model 生成回答
```

### 已实现能力

- 题目多段 chunk 切分
- chunk overlap，降低边界信息丢失
- embedding 维度校验
- pgvector 相似度检索
- distance 阈值过滤
- RAG 决策服务，避免无效 embedding 调用
- 手动单题索引
- 手动批量索引
- 定时索引最近题目

## 本地开发环境

项目使用 Docker Compose 启动本地依赖：

- MySQL
- Redis
- RabbitMQ
- Elasticsearch
- SeaweedFS
- PostgreSQL + pgvector

启动依赖：

```bash
docker compose -f docker-compose.dev.yml up -d
```

启动后端：

```bash
./mvnw spring-boot:run
```

更多 Docker 说明：

- [docs/docker-dev.md](./docs/docker-dev.md)

## 配置说明

AI 相关配置通过环境变量注入，避免把真实 key 提交到仓库。

示例：

```yaml
ai:
  chat-model:
    api-key: ${AI_API_KEY:${OPENAI_API_KEY:}}
    base-url: ${AI_BASE_URL:https://api.deepseek.com}
    model-name: ${AI_MODEL:deepseek-v4-flash}
  embedding-model:
    api-key: ${AI_EMBEDDING_API_KEY:${AI_API_KEY:${OPENAI_API_KEY:}}}
    base-url: ${AI_EMBEDDING_BASE_URL:https://api.openai.com/v1}
    model-name: ${AI_EMBEDDING_MODEL:text-embedding-3-small}
```

RAG 参数也支持配置化：

```yaml
rag:
  chunk:
    max-length: 1200
    overlap-length: 150
  search:
    max-distance: 0.45
  decision:
    min-message-length: 18
  index-schedule:
    enabled: false
```

## 项目文档

- [AI 模块需求分析](./docs/ai-module-requirements.md)
- [AI 实施记录](./docs/ai-implementation-log.md)
- [V4 RAG 需求分析](./docs/ai-v4-rag-requirements.md)
- [Docker 本地开发环境](./docs/docker-dev.md)

## 当前进度

已完成：

- 后端基础业务链路
- 题库、题目、学习行为模块
- Redis 缓存
- RabbitMQ + Elasticsearch 搜索同步
- SeaweedFS 对象存储
- AI 题目追问
- AI 对话记忆、会话摘要、用户长期记忆
- RAG 索引、检索、Prompt 接入
- RAG 批量索引与定时索引

后续可以继续优化：

- RAG Trace 记录
- RAG 离线评估集
- 更细粒度的增量索引
- 搜索高亮和多标签过滤
- 项目部署说明和接口文档整理

## 说明

这个项目主要用于后端开发和 AI 应用工程化学习。代码会随着学习过程持续迭代，文档中保留了较多实现记录，方便复盘每一步为什么这么做。
