# AI V3 用户级长期记忆设计

## 1. V3 做什么

V3 要做的是“用户级长期记忆”。

V2 已经完成的是当前会话内记忆：

```text
当前 session 摘要 + 最近 10 条短期历史
```

它只能解决同一个 session 里的上下文延续。

V3 要解决跨 session 的用户学习画像：

```text
用户 A 在多个 AI 对话里反复暴露出的薄弱点、学习偏好、未解决问题
-> 沉淀成一份 ai_user_memory.memory_summary
```

## 2. V3 不做什么

V3 当前不做 RAG，不做向量检索，不做每条消息级别的记忆判断。

原因：

- 每条消息都调用模型判断是否值得长期记忆，成本高。
- 原始对话噪声多，容易把临时问题写进长期记忆。
- 当前阶段先把“会话摘要 -> 用户长期记忆”这条链路打通。

当前策略是：

```text
原始消息
-> 当前 session 摘要
-> 用户长期记忆
```

## 3. V3.1 做什么：先搭用户记忆基础结构

### 做什么

V3.1 只做三件事：

1. 新增用户长期记忆表。
2. 新增用户长期记忆查询接口。
3. 新增文档和测试。

V3.1 不调用 AI，不自动生成用户记忆。

### 为什么先这么做

因为用户记忆是新的一层数据模型。先把数据结构和查询接口搭好，可以单独验证：

- 表结构是否合理。
- 当前用户是否只能查自己的记忆。
- 没有记忆时接口怎么返回。
- 后续 V3.2 自动更新时应该写到哪里。

### 怎么做

新增表：

```text
ai_user_memory
```

核心字段：

```text
id
user_id
memory_summary
source_session_count
last_source_session_id
create_time
update_time
```

字段含义：

- `user_id`：一位用户对应一份长期学习记忆。
- `memory_summary`：用户级长期学习摘要。
- `source_session_count`：已经合并进用户记忆的不同会话数量。
- `last_source_session_id`：最近一次合并来源会话 ID。

新增接口：

```http
GET /api/ai/user-memory
```

没有记忆时返回：

```json
{
  "hasMemory": false,
  "memorySummary": null,
  "sourceSessionCount": 0,
  "lastSourceSessionId": null
}
```

已有记忆时返回：

```json
{
  "hasMemory": true,
  "memorySummary": "用户在 Java 并发和 MySQL 索引上需要继续巩固",
  "sourceSessionCount": 3,
  "lastSourceSessionId": 2000
}
```

### 改了哪里

新增文件：

- `src/main/resources/migrations/V0.0.15__Add_ai_user_memory_table.sql`
- `src/main/java/com/chushi/aiinterview/entities/AiUserMemory.java`
- `src/main/java/com/chushi/aiinterview/commons/vo/AiUserMemoryVo.java`
- `src/main/java/com/chushi/aiinterview/mappers/AiUserMemoryMapper.java`

修改文件：

- `src/main/java/com/chushi/aiinterview/controller/AiChatController.java`
- `src/main/java/com/chushi/aiinterview/services/AiChatService.java`
- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`
- `src/test/java/com/chushi/aiinterview/services/impl/AiChatServiceImplTest.java`

### 怎么验证

单元测试覆盖：

- 用户没有长期记忆时，返回 `hasMemory=false`。
- 用户已有长期记忆时，返回已有摘要和来源信息。

验证命令：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -Dtest=AiChatServiceImplTest test
```

## 4. V3.2 做什么：用 session 摘要更新用户长期记忆

### 做什么

V3.2 开始自动更新用户长期记忆。

触发点是：

```text
当前 session 摘要更新成功之后
```

完整链路：

```text
用户消息 + AI 回复
-> 旧消息滑出最近 10 条窗口
-> 更新 ai_chat_session.memory_summary
-> 用 session 摘要合并 ai_user_memory.memory_summary
```

### 为什么用 session 摘要来更新

用户长期记忆不直接读每条原始消息，而是读 session 摘要。

原因：

- session 摘要已经压缩过原始对话，信息密度更高。
- 避免每条消息都调用模型判断是否值得长期记忆。
- 降低 token 成本。
- 降低把临时闲聊、重复追问写进长期记忆的概率。

### 怎么做

在 `sendMessage` 的成功链路里：

```text
AI 回复成功
-> 保存 assistant 消息
-> tryRefreshMemorySummary(session, question, currentUser)
```

在 `tryRefreshMemorySummary` 里：

```text
生成当前 session 新摘要
-> 写入 ai_chat_session.memory_summary
-> tryRefreshUserMemory(session, question, currentUser, limitedSummary)
```

用户记忆合并 Prompt 输入：

```text
已有用户长期记忆
本次 session 摘要
本次会话题目信息
```

输出：

```text
新的用户长期学习记忆
```

### 改了哪里

修改文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

新增方法：

```java
private void tryRefreshUserMemory(AiChatSession session, QuestionVo question, User currentUser, String sessionSummary)
```

作用：

- 查询已有用户长期记忆。
- 调用模型生成新的用户长期记忆。
- 如果已有记录，更新 `ai_user_memory`。
- 如果没有记录，插入 `ai_user_memory`。

新增方法：

```java
private String buildUserMemoryPrompt(String currentUserMemory, String sessionSummary, QuestionVo question)
```

作用：

构造用户长期记忆合并 Prompt。

### Prompt 约束

用户长期记忆只保存：

- 用户长期薄弱点。
- 用户反复追问的方向。
- 用户偏好的解释方式。
- 用户尚未解决的问题。

不保存：

- 手机号、邮箱、密钥、验证码等隐私或敏感信息。
- 内部题目 ID、数据库 ID、会话 ID、消息 ID。
- 原始对话逐字内容。

### source_session_count 怎么算

`source_session_count` 表示已经合并进用户记忆的不同会话数量。

如果同一个 session 多次刷新摘要：

- 会更新用户长期记忆内容。
- 不重复增加 `source_session_count`。

如果来源 session 变化：

```text
source_session_count + 1
```

### 失败怎么办

用户长期记忆合并是辅助链路。

如果失败：

- 只写 warn 日志：`AiUserMemoryRefreshException`。
- 不影响本轮聊天回复。
- 不影响当前 session 摘要写入。

### 怎么验证

当前测试被一个非 V3.2 文件阻塞：

```text
src/main/java/com/chushi/aiinterview/publishers/ESMessagePublisher.java:39: <identifier> expected
```

原因是该文件末尾有一行单独的：

```java
HashMap
```

清掉这个语法错误后，再运行：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -Dtest=AiChatServiceImplTest test

env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -DskipTests compile
```

## 5. V3.3 做什么：让用户长期记忆进入聊天 Prompt

### 做什么

V3.3 把 `ai_user_memory.memory_summary` 放进真正的聊天 Prompt。

V3.1 只是建表和查询。

V3.2 只是生成/更新用户长期记忆。

V3.3 才让 AI 回复时读取并使用用户长期记忆。

### 为什么要做这一步

如果只更新 `ai_user_memory`，但聊天时不读取它，那么用户长期记忆只是数据库记录，不会影响 AI 回答。

所以聊天 Prompt 必须从：

```text
当前题目上下文
当前会话长期摘要
最近对话历史
当前用户问题
```

升级成：

```text
当前题目上下文
用户长期学习记忆
当前会话长期摘要
最近对话历史
当前用户问题
```

### 怎么做

在 `sendMessage` 调用模型前，先查询当前用户长期记忆：

```java
var userMemorySummary = aiUserMemoryMapper.findByUserId(currentUser.getId())
        .map(AiUserMemory::getMemorySummary)
        .orElse(null);
```

然后把它传给 `buildPrompt`：

```java
buildPrompt(question, userMemorySummary, session.getMemorySummary(), historyMessages, request.getContent())
```

如果用户还没有长期记忆，Prompt 中该区域显示为：

```text
无
```

### 改了哪里

修改文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

修改内容：

- `sendMessage` 中读取 `ai_user_memory.memory_summary`。
- `buildPrompt` 新增 `userMemorySummary` 参数。
- Prompt 新增 `# 用户长期学习记忆` 区域。
- 原来的 `# 长期记忆摘要` 改名为 `# 当前会话长期摘要`，避免和用户级长期记忆混淆。

### 现在完整 Prompt 结构

```text
当前题目上下文
题目内容
参考答案
用户长期学习记忆
当前会话长期摘要
最近对话历史
当前用户问题
输出要求
```

### 成本影响

V3.3 不额外调用模型。

它只是在聊天前多查一次数据库，把已有用户长期记忆放进 Prompt。

真正的额外模型调用仍然发生在 V3.2：session 摘要更新成功后合并用户长期记忆。

## 6. V3.4 做什么：增强用户记忆调试信息

### 做什么

V3.4 增强 `GET /api/ai/user-memory` 返回值，让接口不只返回记忆内容，还返回当前用户长期记忆的运行策略。

新增字段：

```json
{
  "promptEnabled": true,
  "updateStrategy": "session_summary",
  "maxMemoryLength": 3000
}
```

### 为什么要做

V3.3 已经让用户长期记忆进入聊天 Prompt。

但只看 `memorySummary`，无法直接知道：

- 这份记忆现在是否已经参与 Prompt。
- 用户记忆是怎么更新的。
- 当前记忆最大长度是多少。

所以调试接口需要把这些策略直接返回。

### 字段含义

- `promptEnabled`：当前用户长期记忆是否会进入聊天 Prompt。现在固定为 `true`。
- `updateStrategy`：用户记忆更新策略。现在是 `session_summary`，表示基于 session 摘要更新，不是每条消息更新。
- `maxMemoryLength`：用户长期记忆最大长度。现在是 `3000` 字符。

### 改了哪里

修改文件：

- `src/main/java/com/chushi/aiinterview/commons/vo/AiUserMemoryVo.java`
- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`
- `src/test/java/com/chushi/aiinterview/services/impl/AiChatServiceImplTest.java`

### 怎么验证

单元测试覆盖：

- 用户没有长期记忆时，也返回当前策略字段。
- 用户已有长期记忆时，也返回当前策略字段。

验证命令：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -Dtest=AiChatServiceImplTest test
```

## 7. V3.5 做什么：真实接口联调验证用户长期记忆

### 做什么

V3.5 不新增业务功能，重点是用真实后端、真实数据库、真实模型调用验证 V3 链路。

验证链路：

```text
发送多轮 AI 对话
-> 触发当前 session 摘要
-> session 摘要合并进 ai_user_memory
-> GET /api/ai/user-memory 返回 hasMemory=true
-> 后续聊天 Prompt 已具备读取用户长期记忆的能力
```

### 验证结果

日期：2026-06-15。

临时启动当前后端：

```text
server.port = 18080
spring.profiles.active = dev
```

Flyway 执行结果：

```text
schema interview: 0.0.14 -> 0.0.15
V0.0.15__Add_ai_user_memory_table.sql applied
```

测试会话：

```text
sessionId = 93371710622928896
```

连续发送 7 轮消息，全部成功。

最终 session 记忆状态：

```text
successMessageCount = 14
recentMessageCount = 10
memorySummaryLength = 886
pendingSummaryMessageCount = 0
```

最终用户长期记忆状态：

```text
hasMemory = true
sourceSessionCount = 1
lastSourceSessionId = 93371710622928896
memorySummaryLength = 1538
promptEnabled = true
updateStrategy = session_summary
maxMemoryLength = 3000
```

数据库确认：

```text
ai_user_memory.memory_summary 已写入
memory length = 1538
source_session_count = 1
last_source_session_id = 93371710622928896
```

### 结论

V3 链路验证通过：

- V3.1 用户长期记忆表和查询接口可用。
- V3.2 session 摘要可以合并进用户长期记忆。
- V3.3 用户长期记忆已经进入聊天 Prompt。
- V3.4 调试接口可以展示当前策略。

