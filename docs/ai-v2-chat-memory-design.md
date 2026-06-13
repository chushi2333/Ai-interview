# AI V2 Chat Memory 设计文档

## 1. V2 要解决什么问题

V1 已经完成了两个能力：

- 单次 AI 题解助教：用户选择一个类型，后端拼题目上下文，调用模型，返回 `content`。
- AI 调用记录：保存谁在什么时候、对哪道题、用什么类型调用了 AI，以及成功/失败、耗时和返回内容。

但是 V1 仍然是“单次问答”。每次请求之间没有上下文关系。

V2 要做的是“围绕一道题的多轮对话”。用户可以在同一道题下面连续追问，AI 需要知道前面聊过什么。

示例：

```text
用户：解释一下 JVM、JRE、JDK 的区别。
AI：...
用户：那面试时怎么回答更自然？
AI：基于上一轮内容，组织一版面试回答。
用户：能不能再给我 3 个追问？
AI：继续围绕前面主题生成追问。
```

这才需要 Chat Memory。

## 2. V2 的边界

V2 做：

- 创建题目 AI 对话会话。
- 保存用户消息和 AI 消息。
- 调用模型时带上最近几轮历史消息。
- 支持查询某个会话的消息列表。
- 支持围绕同一道题继续追问。

V2 不做：

- 不做 RAG。
- 不做 MCP。
- 不做 Tool Calling。
- 不做跨题目的长期记忆。
- 不让 AI 自动修改题目、题解、错题本、收藏等业务数据。
- 不做复杂 Agent 流程。

## 3. Chat Memory 和调用记录的区别

### 调用记录

V1.1 的 `ai_assist_record` 是审计记录。

用途：

- 看用户调用过什么。
- 排查模型调用失败。
- 统计接口使用情况。
- 学习复盘。

特点：

- 不参与下一次 Prompt。
- 不影响模型回答。
- 主要服务工程排查和产品历史。

### Chat Memory

V2 的 Chat Memory 是对话上下文。

用途：

- 让模型知道上一轮用户问了什么。
- 让模型能基于前文继续回答。
- 支持连续追问。

特点：

- 会参与下一次模型调用。
- 会影响模型回答。
- 需要控制历史长度，避免 token 太长。

一句话区别：

```text
调用记录是“事后留痕”，Chat Memory 是“下次回答要带上的上下文”。
```

## 4. Chat Memory 和 RAG 的区别

Chat Memory 解决的是“当前会话前面聊过什么”。

RAG 解决的是“从大量外部资料里检索哪些内容再回答”。

当前 V2 只围绕一道题继续追问，题目上下文可以直接通过 `questionId` 查出来，不需要向量检索。

因此 V2 不需要 RAG。

后续如果出现这些需求，才考虑 RAG：

- 用户问的问题需要跨多个题目查资料。
- AI 需要从题库、题解、笔记、文档中找相关片段。
- AI 需要引用多个知识点进行总结。
- AI 需要根据用户学习历史推荐题目。

## 5. 产品流程

### 5.1 创建或进入题目对话

用户在题目详情页点击“AI 连续追问”或“对话助教”。

前端可以：

- 如果没有会话，调用创建会话接口。
- 如果已有会话，直接进入最近一个会话。

### 5.2 用户发送消息

用户输入一个问题，例如：

```text
这道题面试时怎么回答？
```

后端做：

1. 校验用户登录和题目权限。
2. 保存用户消息。
3. 查询最近 N 条历史消息。
4. 查询题目上下文。
5. 组装系统 Prompt、题目上下文、历史消息、本次用户消息。
6. 调用模型。
7. 保存 AI 回复消息。
8. 返回 AI 回复。

### 5.3 查询历史消息

用户刷新页面或重新进入会话时，前端查询消息列表。

后端按时间顺序返回消息。

## 6. 数据库设计

V2 建议新增两张表：

- `ai_chat_session`
- `ai_chat_message`

### 6.1 ai_chat_session

用于保存一次围绕题目的 AI 对话会话。

建议字段：

```sql
CREATE TABLE `ai_chat_session`
(
    `id`          BIGINT       NOT NULL COMMENT 'AI对话会话ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `question_id` BIGINT       NOT NULL COMMENT '题目ID',
    `title`       VARCHAR(128) NULL COMMENT '会话标题',
    `status`      VARCHAR(32)  NOT NULL COMMENT '会话状态：active活跃 archived归档',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='AI对话会话表'
  ROW_FORMAT = Dynamic;
```

索引建议：

```sql
CREATE INDEX `idx_acs_user_question_id`
    ON `ai_chat_session` (`user_id`, `question_id`, `id`);

CREATE INDEX `idx_acs_user_id`
    ON `ai_chat_session` (`user_id`, `id`);
```

字段说明：

- `id`：雪花 ID。
- `user_id`：会话属于哪个用户。
- `question_id`：会话围绕哪道题。
- `title`：会话标题，第一版可以用题目标题或用户第一句话截断生成。
- `status`：第一版只用 `active`，后续可以扩展归档。
- `create_time`：创建时间。
- `update_time`：最后更新时间。

为什么需要 session 表：

- 一道题下可能有多次对话。
- 后续可以展示“历史会话列表”。
- 后续可以归档、删除、重命名会话。

### 6.2 ai_chat_message

用于保存每一条对话消息。

建议字段：

```sql
CREATE TABLE `ai_chat_message`
(
    `id`            BIGINT        NOT NULL COMMENT 'AI对话消息ID',
    `session_id`    BIGINT        NOT NULL COMMENT '会话ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `question_id`   BIGINT        NOT NULL COMMENT '题目ID',
    `role`          VARCHAR(32)   NOT NULL COMMENT '消息角色：user assistant system',
    `content`       MEDIUMTEXT    NOT NULL COMMENT '消息内容',
    `model_name`    VARCHAR(128)  NULL COMMENT '模型名称，仅assistant消息需要',
    `status`        VARCHAR(32)   NOT NULL COMMENT '消息状态：success failed',
    `error_message` VARCHAR(1024) NULL COMMENT '失败错误信息',
    `latency_ms`    BIGINT        NULL COMMENT 'AI回复耗时，单位毫秒',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='AI对话消息表'
  ROW_FORMAT = Dynamic;
```

索引建议：

```sql
CREATE INDEX `idx_acm_session_id`
    ON `ai_chat_message` (`session_id`, `id`);

CREATE INDEX `idx_acm_user_question_id`
    ON `ai_chat_message` (`user_id`, `question_id`, `id`);
```

字段说明：

- `session_id`：消息属于哪个会话。
- `user_id`：冗余保存用户 ID，方便权限过滤和查询。
- `question_id`：冗余保存题目 ID，方便按题目查消息。
- `role`：区分用户消息和 AI 消息。
- `content`：消息正文。
- `model_name`：AI 回复使用的模型名，用户消息为空。
- `status`：消息保存状态。用户消息通常是 `success`。
- `error_message`：AI 调用失败时保存。
- `latency_ms`：AI 回复耗时。

为什么 message 表也保存 `user_id` 和 `question_id`：

理论上可以通过 session 表 JOIN 查到，但冗余保存能让常见查询更简单，也方便后续做统计。

## 7. 接口设计

### 7.1 创建会话

```http
POST /api/ai/question/{questionId}/chat/sessions
```

请求体：

```json
{
  "title": "JDK 和 JRE 的区别"
}
```

`title` 可选。

返回：

```json
{
  "id": 123,
  "question_id": 81824214700527616,
  "question_title": "JDK、JRE、JVM 的区别",
  "title": "JDK 和 JRE 的区别",
  "status": "active",
  "create_time": "2026-06-13T10:00:00"
}
```

### 7.2 查询题目下的会话列表

```http
GET /api/ai/question/{questionId}/chat/sessions?last_id=&size=10
```

用途：

- 前端展示该题目下的历史 AI 对话。
- 用户可以选择继续某个旧会话。

### 7.3 发送对话消息

```http
POST /api/ai/chat/sessions/{sessionId}/messages
```

请求体：

```json
{
  "content": "这道题面试时怎么回答？"
}
```

返回：

```json
{
  "user_message": {
    "id": 1,
    "role": "user",
    "content": "这道题面试时怎么回答？"
  },
  "assistant_message": {
    "id": 2,
    "role": "assistant",
    "content": "可以按以下结构回答...",
    "model_name": "deepseek-v4-flash",
    "latency_ms": 1200
  }
}
```

为什么发送消息接口用 `sessionId`：

- 对话必须属于一个会话。
- 会话已经绑定了 `questionId`，发送消息时不需要重复传 `questionId`。
- 后端可以通过 session 查题目上下文和权限。

### 7.4 查询会话消息列表

```http
GET /api/ai/chat/sessions/{sessionId}/messages?last_id=&size=20
```

返回：

```json
{
  "messages": [
    {
      "id": 1,
      "role": "user",
      "content": "这道题面试时怎么回答？",
      "create_time": "2026-06-13T10:00:00"
    },
    {
      "id": 2,
      "role": "assistant",
      "content": "可以按以下结构回答...",
      "model_name": "deepseek-v4-flash",
      "latency_ms": 1200,
      "create_time": "2026-06-13T10:00:01"
    }
  ]
}
```

第一版查询可以按 `id DESC` 分页，前端展示前再倒序；也可以直接按 `id ASC` 返回最近 N 条。实现时优先跟项目现有游标分页风格保持一致。

## 8. Prompt 设计

V2 的 Prompt 由四部分组成：

1. 系统角色。
2. 当前题目上下文。
3. 最近几轮历史消息。
4. 当前用户消息。

系统角色示例：

```text
你是一个面向程序员面试刷题场景的 AI 对话助教。
你只能围绕当前题目和用户的学习问题回答。
如果用户问题偏离当前题目，请简短回答后引导回当前题目。
如果题目上下文不足，请明确说明缺少哪些信息，不要编造事实。
```

题目上下文：

```text
题目 ID：...
题目标题：...
所属题库：...
难度：...
标签：...
题目内容：...
参考答案：...
```

历史消息：

```text
用户：上一轮问题
助教：上一轮回答
用户：再上一轮问题
助教：再上一轮回答
```

当前用户消息：

```text
用户当前问题：...
```

## 9. 历史消息长度控制

不能把所有历史消息无限传给模型。

第一版建议：

- 只取最近 10 条消息。
- 或者最近 5 轮对话，也就是 5 条 user + 5 条 assistant。
- 单条用户输入限制 2000 字符。
- 如果后续消息太长，再做摘要记忆。

为什么要限制：

- 模型上下文长度有限。
- 历史越长，响应越慢，成本越高。
- 太多历史可能干扰当前题目回答。

## 10. LangChain4j 使用方式

V2 有两种实现方式。

### 方式一：手动组装 Prompt

做法：

- 从数据库查最近几条消息。
- 手动拼接成文本。
- 调用当前已有的 `ChatModel.chat(prompt)`。

优点：

- 最容易理解。
- 和 V1 代码衔接最顺。
- 方便学习每一步上下文是怎么进入模型的。

缺点：

- 还没有真正用到 LangChain4j 的 Chat Memory 抽象。

### 方式二：使用 LangChain4j Chat Memory

做法：

- 把数据库消息转换成 LangChain4j message。
- 放入 Chat Memory。
- 通过 AI Service 或 ChatModel messages API 调用。

优点：

- 更贴近 LangChain4j 的标准做法。
- 后续接 AI Service 更自然。

缺点：

- 对初学者来说抽象更多。
- 需要先理解 LangChain4j 的 message、memory、AI Service 概念。

V2 建议选择：

```text
第一版先手动组装历史消息，跑通持久化 Chat Memory 的业务闭环。
下一步再重构为 LangChain4j Chat Memory / AI Service。
```

原因：

学习目标是从零开始。先手写一版，能看清楚“历史消息到底怎么影响模型回答”。之后再引入框架抽象，理解会更扎实。

## 11. 后端实现步骤

### Step 1：新增 migration

新增：

- `V0.0.12__Add_ai_chat_tables.sql`

内容：

- `ai_chat_session`
- `ai_chat_message`
- 索引

### Step 2：新增实体

新增：

- `AiChatSession`
- `AiChatMessage`

### Step 3：新增 VO / DTO

DTO：

- `AiChatSessionCreateDto`
- `AiChatMessageCreateDto`

VO：

- `AiChatSessionVo`
- `AiChatSessionListVo`
- `AiChatMessageVo`
- `AiChatMessageListVo`
- `AiChatMessageSendVo`

### Step 4：新增 Mapper

新增：

- `AiChatSessionMapper`
- `AiChatMessageMapper`
- `AiChatSessionMapper.xml`
- `AiChatMessageMapper.xml`

### Step 5：新增 Service

新增：

- `AiChatService`
- `AiChatServiceImpl`

主要方法：

```java
AiChatSessionVo createQuestionChatSession(Long questionId, AiChatSessionCreateDto request, User currentUser);
List<AiChatSessionVo> getQuestionChatSessionList(Long questionId, User currentUser, Long lastId, Integer size);
AiChatMessageSendVo sendMessage(Long sessionId, AiChatMessageCreateDto request, User currentUser);
List<AiChatMessageVo> getMessageList(Long sessionId, User currentUser, Long lastId, Integer size);
```

### Step 6：新增 Controller

新增：

- `AiChatController`

接口：

```http
POST /api/ai/question/{questionId}/chat/sessions
GET  /api/ai/question/{questionId}/chat/sessions
POST /api/ai/chat/sessions/{sessionId}/messages
GET  /api/ai/chat/sessions/{sessionId}/messages
```

### Step 7：组装上下文并调用模型

发送消息时：

1. 查询 session。
2. 校验 session 属于当前用户。
3. 查询题目详情。
4. 保存 user message。
5. 查询最近历史消息。
6. 拼 Prompt。
7. 调用模型。
8. 保存 assistant message。
9. 返回 user message + assistant message。

### Step 8：验证

编译验证：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH ./mvnw -q -DskipTests compile
```

运行期验证：

1. 启动 Docker 依赖。
2. 启动后端 dev profile。
3. 确认 Flyway 迁移成功。
4. 登录获取 token。
5. 创建会话。
6. 发送第一条消息。
7. 发送第二条追问。
8. 查询消息列表，确认 user 和 assistant 消息都保存。
9. 确认第二轮回答能承接第一轮上下文。

## 12. 第一版验收标准

V2 完成后，应该满足：

- 用户可以基于某道题创建 AI 对话会话。
- 用户可以在会话里连续发送消息。
- AI 回复会结合当前题目上下文。
- AI 回复会结合最近几轮历史消息。
- 用户消息和 AI 消息都会保存到数据库。
- 可以查询会话列表。
- 可以查询消息列表。
- 权限不会越权：用户不能访问别人的会话和消息。
- 编译通过。
- 运行期接口验证通过。
- 文档记录完整。

## 13. 暂不解决的问题

这些留到后续版本：

- 历史消息摘要。
- 会话重命名。
- 会话删除或归档。
- 前端流式输出。
- 模型调用取消。
- token 预算精细计算。
- LangChain4j AI Service 重构。
- LangChain4j Chat Memory Store 抽象。
- RAG 检索题库资料。
- MCP 工具接入。

## 14. 下一步

确认这个设计后，下一步开始编码：

1. 新增 `V0.0.12__Add_ai_chat_tables.sql`。
2. 新增实体和 Mapper。
3. 先完成创建会话和查询会话。
4. 再完成发送消息和保存 AI 回复。
5. 最后做运行期验证并补充实现日志。


## 15. 第一版实现记录

时间：2026-06-13

第一版已经按本文档实现，当前采用“手动组装 Prompt + 数据库持久化消息”的方式。

### 已新增文件

数据库迁移：

- `src/main/resources/migrations/V0.0.12__Add_ai_chat_tables.sql`

实体：

- `src/main/java/com/chushi/aiinterview/entities/AiChatSession.java`
- `src/main/java/com/chushi/aiinterview/entities/AiChatMessage.java`

DTO：

- `src/main/java/com/chushi/aiinterview/commons/dto/AiChatSessionCreateDto.java`
- `src/main/java/com/chushi/aiinterview/commons/dto/AiChatMessageCreateDto.java`

VO：

- `src/main/java/com/chushi/aiinterview/commons/vo/AiChatSessionVo.java`
- `src/main/java/com/chushi/aiinterview/commons/vo/AiChatSessionListVo.java`
- `src/main/java/com/chushi/aiinterview/commons/vo/AiChatMessageVo.java`
- `src/main/java/com/chushi/aiinterview/commons/vo/AiChatMessageListVo.java`
- `src/main/java/com/chushi/aiinterview/commons/vo/AiChatMessageSendVo.java`

Mapper：

- `src/main/java/com/chushi/aiinterview/mappers/AiChatSessionMapper.java`
- `src/main/java/com/chushi/aiinterview/mappers/AiChatMessageMapper.java`
- `src/main/resources/mappers/AiChatSessionMapper.xml`
- `src/main/resources/mappers/AiChatMessageMapper.xml`

Service / Controller：

- `src/main/java/com/chushi/aiinterview/services/AiChatService.java`
- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`
- `src/main/java/com/chushi/aiinterview/controller/AiChatController.java`

公共组件：

- `src/main/java/com/chushi/aiinterview/components/AiChatModelProvider.java`

### 关键实现点

发送消息时的顺序是：

1. 查询 session 并校验归属用户。
2. 查询题目详情并复用题目权限校验。
3. 查询旧历史消息。
4. 保存当前 user 消息。
5. 组装 Prompt。
6. 调用模型。
7. 保存 assistant 消息。
8. 返回 user message 和 assistant message。

这里先查旧历史，再保存当前 user 消息，是为了避免当前问题重复进入 Prompt。

错误示例：

```text
最近对话历史：用户当前问题
当前用户问题：用户当前问题
```

正确结果：

```text
最近对话历史：上一轮用户问题 + 上一轮助教回答
当前用户问题：本次用户问题
```

### 验证结果

编译验证通过。

运行期验证通过：

- Flyway 成功迁移到 `0.0.12`。
- `ai_chat_session` 和 `ai_chat_message` 表创建成功。
- 创建会话接口通过。
- 连续两轮消息发送通过。
- 查询消息列表通过，共 4 条消息。
- 查询会话列表通过。

### 当前保留的限制

- 消息列表按 `id DESC` 返回，最新消息在前。
- 第一版只带最近 10 条成功消息进入 Prompt。
- 第一版还没有做摘要记忆。
- 第一版还没有改成 LangChain4j 的 Chat Memory Store 抽象。
- 第一版还没有做流式输出。


## 16. V2.1 消息列表返回顺序

第一版查询消息列表时，Mapper 使用：

```sql
ORDER BY id DESC
```

这样做适合游标分页，因为可以快速拿到最新一页消息。

但是前端聊天窗口通常按时间正序展示，也就是旧消息在上，新消息在下。

因此 V2.1 的规则是：

```text
数据库查询：id DESC，拿最新一页
接口返回：Service 里 reverse，变成时间正序
```

这样兼顾了两件事：

- 数据库分页查询简单高效。
- 前端可以直接从上到下渲染消息。

关键代码在 `AiChatServiceImpl#getMessageList`：

```java
var messages = new ArrayList<>(aiChatMessageMapper.findMessageListBySessionId(sessionId, currentUser.getId(), lastId, size));
Collections.reverse(messages);
return messages;
```

## 17. V2.2 会话管理

V2.2 解决的是“对话越来越多以后怎么管理”的问题。

V2 第一版已经有了 session 和 message，但 session 只负责承载消息，还不像一个可长期使用的历史对话列表。用户如果围绕同一道题开了多次追问，很难从一堆默认标题里区分每次对话。

### 设计目标

- 用户可以重命名会话。
- 用户可以删除不需要的会话。
- 新会话在第一次追问后可以自动变成更有区分度的标题。
- 删除会话不物理删除消息，避免误删数据，也方便后续审计或恢复。

### 新增接口

```http
PUT /api/ai/chat/sessions/{sessionId}
DELETE /api/ai/chat/sessions/{sessionId}
```

`PUT` 用于修改标题，请求体：

```json
{
  "title": "HashMap 扩容追问"
}
```

`DELETE` 用于软删除会话。

### 软删除规则

`ai_chat_session` 新增字段：

```sql
is_delete TINYINT NOT NULL DEFAULT 0
```

规则：

- `0`：未删除。
- `1`：已删除。
- 会话列表只查询 `is_delete = 0`。
- `findById` 只查询 `is_delete = 0`，所以删除后的会话不能继续发送消息，也不能继续查看消息。
- `ai_chat_message` 不做物理删除。

### 重命名规则

标题约束：

- 不能为空。
- 最长 128 字符。
- 修改时必须校验 session 属于当前用户。

### 自动标题规则

触发时机：

- 发送第一条用户消息后。
- 调用模型之前。

触发条件：

- 会话没有历史消息。
- 当前标题还是默认标题。

默认标题包括：

- `AI 对话`
- 当前题目标题
- `AI 追问：当前题目标题`

生成方式：

```text
追问： + 第一条用户问题
```

处理细节：

- 去掉换行和制表符。
- 压缩连续空白。
- 最长 128 字符。

这里先不调用模型生成标题，因为标题生成属于体验优化，不应该让一次普通追问额外产生第二次模型调用。

### 文件变更

- `AiChatSessionUpdateDto`：重命名请求体。
- `AiChatController`：新增 PUT / DELETE 接口。
- `AiChatService`：新增重命名和删除方法。
- `AiChatServiceImpl`：实现重命名、软删除、自动标题。
- `AiChatSessionMapper`：新增 `updateTitle`、`softDelete`。
- `AiChatSessionMapper.xml`：会话列表过滤已删除会话。
- `AiChatSession`：新增 `isDelete` 字段。
- `V0.0.13__Alter_ai_chat_session_add_is_delete.sql`：新增软删除字段和索引。

