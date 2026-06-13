# AI V1 题解助教实现笔记

本文档按“每一步做了什么 + 为什么这么做 + 代码怎么看”的方式记录 AI V1 接入过程，方便后续复盘和学习。

## V1 总目标

V1 只做 AI 题解助教，不做 RAG、MCP、Tool Calling、多轮会话和流式输出。

目标链路：

```text
前端请求 questionId + assist type
        ↓
Controller 接收请求
        ↓
Service 复用 QuestionService 查询当前题目详情
        ↓
根据题目上下文和 assist type 构造 Prompt
        ↓
LangChain4j 调用大模型
        ↓
返回 content 文本
```

## Step 1：新增 LangChain4j 依赖

涉及文件：

- `pom.xml`

新增版本号：

```xml
<langchain4j.version>1.15.1</langchain4j.version>
```

新增依赖：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

作用：

- 引入 LangChain4j 的 OpenAI 兼容模型客户端。
- 后端可以通过 `OpenAiChatModel` 调用 OpenAI 或兼容 OpenAI API 格式的模型服务。

为什么先用 `langchain4j-open-ai`：

- V1 只需要最基础的模型调用。
- 不需要 Spring Boot 自动创建模型 Bean。
- 可以自己控制 API Key 缺失时的错误提示。
- 更适合学习 LangChain4j 最底层的模型调用流程。

这一步没有做什么：

- 没有引入 RAG 相关依赖。
- 没有引入 MCP 相关依赖。
- 没有引入 LangChain4j AI Service 自动代理能力。

## Step 2：新增参数校验依赖

涉及文件：

- `pom.xml`

新增依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

作用：

- 支持 `@Valid`、`@NotBlank`、`@Size` 等请求参数校验注解。
- AI 接口请求体需要校验 `type` 不能为空、`userInput` 不能过长。

为什么需要：

- AI 请求如果不做校验，非法 type 或超长输入会直接进入业务逻辑。
- Prompt 输入过长会增加模型调用成本，也可能影响输出稳定性。

## Step 3：新增 AI 配置项

涉及文件：

- `src/main/resources/application.yaml`
- `src/main/resources/application-dev.yaml`

新增配置：

```yaml
ai:
  chat-model:
    api-key: ${AI_API_KEY:${OPENAI_API_KEY:}}
    base-url: ${AI_BASE_URL:https://api.deepseek.com}
    model-name: ${AI_MODEL:deepseek-v4-flash}
    temperature: ${AI_TEMPERATURE:0.2}
    timeout: ${AI_TIMEOUT:60s}
    log-requests: ${AI_LOG_REQUESTS:false}
    log-responses: ${AI_LOG_RESPONSES:false}
```

每个配置项的含义：

- `api-key`：模型服务的 API Key，优先从环境变量 `AI_API_KEY` 读取，并兼容旧的 `OPENAI_API_KEY`，默认空。
- `base-url`：模型服务地址，默认 DeepSeek OpenAI 兼容 API 地址。
- `model-name`：模型名称，默认 `deepseek-v4-flash`。
- `temperature`：生成随机性，V1 设置为 `0.2`，让回答更稳定。
- `timeout`：模型调用超时时间，默认 60 秒。
- `log-requests`：是否打印模型请求日志，默认关闭，避免泄露 Prompt 或敏感信息。
- `log-responses`：是否打印模型响应日志，默认关闭。

为什么 API Key 默认空：

- 保证项目没有 API Key 时仍然可以启动。
- 真正调用 AI 接口时再提示 `AI api key is not configured`。
- 避免本地开发、普通后端接口、Swagger 启动都被 AI 配置阻塞。

## Step 4：新增配置映射类 `AiProperties`

涉及文件：

- `src/main/java/com/chushi/aiinterview/configurations/AiProperties.java`

核心代码：

```java
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private ChatModelProperties chatModel = new ChatModelProperties();
}
```

作用：

- 把 YAML 中的 `ai.chat-model.*` 配置绑定成 Java 对象。
- Service 层不用直接读环境变量或解析 YAML。
- 后续更换模型提供商、模型名、baseUrl 时，不需要改业务代码。

内部配置类：

```java
public static class ChatModelProperties {
    private String apiKey;
    private String baseUrl;
    private String modelName;
    private Double temperature;
    private Duration timeout;
    private Boolean logRequests;
    private Boolean logResponses;
}
```

这里不在 Java 类里写默认值。默认值统一放在 YAML 中，例如：

```yaml
model-name: ${AI_MODEL:deepseek-v4-flash}
```

这样做的原因：

- 配置文件里能直接看到所有可调参数。
- Java 类只负责映射配置结构，不承担配置默认值。
- 避免 Java 类和 YAML 两边同时写默认值，后续改模型时不容易漏改。
- API Key 不能写进代码或 Git。
- 模型名可能经常调整。
- 本地、测试、生产可能使用不同模型服务地址。

## Step 5：启用配置属性绑定

涉及文件：

- `src/main/java/com/chushi/aiinterview/configurations/ApplicationConfiguration.java`

新增注解：

```java
@EnableConfigurationProperties(AiProperties.class)
```

作用：

- 告诉 Spring Boot 启用 `AiProperties` 配置绑定。
- 这样其他 Bean 才能通过 `@Resource` 注入 `AiProperties`。

## Step 6：新增 AI 助教类型枚举

涉及文件：

- `src/main/java/com/chushi/aiinterview/commons/enums/AiQuestionAssistType.java`

支持类型：

```text
simple_explain
interview_answer
key_points
follow_up_questions
answer_polish
```

作用：

- 限制 V1 只支持这 5 种任务。
- 把前端传入的字符串转换成后端枚举。
- 非法 type 直接返回业务错误。

关键方法：

```java
public static AiQuestionAssistType fromValue(String value)
```

这个方法做了什么：

- 遍历枚举列表。
- 找到和请求 `type` 一致的枚举。
- 找不到就抛出 `BusinessException(400, "Unsupported AI assist type")`。

为什么不用前端随便传 prompt：

- V1 不是通用聊天。
- 后端必须控制 AI 能做什么。
- 固定 type 更容易控制 Prompt、输出格式和成本。

## Step 7：新增请求 DTO

涉及文件：

- `src/main/java/com/chushi/aiinterview/commons/dto/AiQuestionAssistRequestDto.java`

字段：

```java
@NotBlank(message = "type must not be empty")
private String type;

@Size(max = 2000, message = "user input length must be less than 2000")
private String userInput;
```

作用：

- 接收 AI 助教接口请求体。
- `type` 决定本次 AI 助教任务。
- `userInput` 是用户补充说明，例如“我不理解缓存击穿”。

注意：

项目配置了 Jackson `SNAKE_CASE`，所以前端 JSON 可以传：

```json
{
  "type": "simple_explain",
  "user_input": "我不理解缓存击穿"
}
```

后端会映射到 Java 字段 `userInput`。

## Step 8：新增响应 VO

涉及文件：

- `src/main/java/com/chushi/aiinterview/commons/vo/AiQuestionAssistVo.java`

字段：

```java
private String content;
```

作用：

- V1 先返回纯文本内容。
- 前端直接展示 `content`。

为什么 V1 不做复杂结构：

- 第一阶段重点是打通模型调用链路。
- 结构化输出放到 V2。
- 先用最简单的响应降低调试成本。

## Step 9：新增 Service 接口

涉及文件：

- `src/main/java/com/chushi/aiinterview/services/AiQuestionAssistService.java`

方法：

```java
AiQuestionAssistVo assistQuestion(Long questionId, AiQuestionAssistRequestDto request, User currentUser);
```

参数含义：

- `questionId`：当前题目 ID。
- `request`：AI 助教请求体。
- `currentUser`：当前登录用户，用于复用题目权限逻辑。

为什么需要 Service 接口：

- Controller 只负责 HTTP 请求和响应。
- 业务逻辑放 Service，符合当前项目结构。
- 后续可以替换实现，例如改成 AI Service、流式输出或异步任务。

## Step 10：新增 Service 实现

涉及文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiQuestionAssistServiceImpl.java`

核心流程：

```java
var assistType = AiQuestionAssistType.fromValue(request.getType());
var question = questionService.getQuestionById(questionId, currentUser);
var prompt = buildPrompt(question, assistType, request.getUserInput());
return new AiQuestionAssistVo(getChatModel().chat(prompt));
```

每一步解释：

1. 解析 `type`，确认本次任务是否合法。
2. 复用 `QuestionService.getQuestionById` 查询题目详情。
3. 使用题目标题、内容、答案、标签、题库信息构造 Prompt。
4. 调用 LangChain4j 的 `ChatModel.chat(prompt)`。
5. 把模型文本包装成 `AiQuestionAssistVo` 返回。

为什么复用 `QuestionService.getQuestionById`：

- 它已经处理题目不存在。
- 它已经处理会员题权限。
- 它会返回带答案的 `QuestionVo`。
- 避免 AI 模块绕过题目模块的业务规则。

模型创建方式：

```java
OpenAiChatModel.builder()
    .apiKey(config.getApiKey())
    .baseUrl(config.getBaseUrl())
    .modelName(config.getModelName())
    .temperature(config.getTemperature())
    .timeout(config.getTimeout())
    .logRequests(config.getLogRequests())
    .logResponses(config.getLogResponses())
    .build();
```

为什么懒加载模型：

- 应用启动时不强制要求 API Key。
- 第一次真正调用 AI 接口时才创建模型。
- 创建后缓存到 `chatModel` 字段，后续请求复用。

缺少 API Key 时：

```java
throw new BusinessException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "AI api key is not configured");
```

这会返回 HTTP 503，说明 AI 服务当前不可用，但不是系统代码崩溃。

## Step 11：Prompt 构造规则

涉及文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiQuestionAssistServiceImpl.java`

Prompt 包含几块：

- AI 角色：程序员面试刷题场景的题解助教。
- 约束：只能基于题目上下文回答，不能编造。
- 任务：由 `AiQuestionAssistType` 决定。
- 题目上下文：ID、标题、题库、难度、标签、题目内容、参考答案。
- 用户补充输入。
- 输出要求。

为什么这样设计：

- 明确场景，避免变成通用聊天。
- 明确上下文，减少模型乱答。
- 按 type 增加输出指令，让不同任务输出更稳定。

不同 type 的输出指令：

- `simple_explain`：用大白话解释核心概念，并给开发场景例子。
- `interview_answer`：按“简短结论、核心原理、实际场景、注意点”组织。
- `key_points`：列出 3 到 6 个重点。
- `follow_up_questions`：生成 5 个追问，并标注考察点。
- `answer_polish`：指出用户回答问题，再给改写版本。

## Step 12：新增 Controller

涉及文件：

- `src/main/java/com/chushi/aiinterview/controller/AiQuestionAssistController.java`

接口：

```http
POST /api/ai/question/{questionId}/assist
```

方法：

```java
public Response<AiQuestionAssistVo> assistQuestion(
        @PathVariable Long questionId,
        @Valid @RequestBody AiQuestionAssistRequestDto request,
        @CurrentUser User currentUser
)
```

作用：

- 接收 HTTP 请求。
- 通过 `@CurrentUser` 获取当前用户。
- 通过 `@Valid` 校验请求体。
- 调用 `AiQuestionAssistService`。
- 使用 `BaseController.wrap(...)` 返回统一响应结构。

权限：

```java
@RequireRole(value = {UserRole.USER, UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
```

说明：

- 普通用户、管理员、超级管理员都可以使用 AI 助教。
- 未登录用户不能调用。
- 具体题目权限继续由 `QuestionService.getQuestionById` 处理。

## Step 13：编译验证

命令：

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH ./mvnw -q -DskipTests compile
```

为什么指定 JDK 17：

- 项目 `pom.xml` 设置 `java.version` 为 17。
- 当前 shell 默认 Java 是 8。
- 项目已有代码使用了 Java 17 语法，例如 text block、record、pattern matching for instanceof。
- IDEA 能运行项目，是因为 IDEA 使用了 JDK 17；终端环境需要手动指定。

验证结果：

- 使用 Java 8 编译失败，原因是 JDK 版本不匹配。
- 使用 `/usr/lib/jvm/java-17-openjdk-amd64` 编译通过。

## 当前 V1 尚未完成的验证

还需要实际接口验证：

- 无 API Key 调用时返回 `AI api key is not configured`。
- 有 API Key 调用时能返回模型内容。
- 非法 type 返回 `Unsupported AI assist type`。
- 不存在的 questionId 返回题目不存在错误。

这些属于运行期验证，下一步执行。

## Step 14：配置默认值放在哪里

调整原因：

最初 `AiProperties.ChatModelProperties` 里给 `baseUrl`、`modelName`、`temperature` 等字段写了 Java 默认值，同时 YAML 里也写了默认值。这样虽然能运行，但学习和维护时容易困惑：到底应该改 Java 类，还是改配置文件。

最终规则：

- Java 配置类只做结构映射。
- 默认值统一写在 `application.yaml` 和 `application-dev.yaml`。
- 环境变量仍然可以覆盖 YAML 默认值。

最终优先级：

```text
环境变量 / 启动参数 > application-dev.yaml > application.yaml > Java 字段默认值
```

现在 Java 字段不再写业务默认值，因此主要看 YAML。


## Step 15：切换默认模型到 DeepSeek V4 Flash

调整原因：

V1 代码使用的是 LangChain4j 的 `OpenAiChatModel`，它并不只能连接 OpenAI 官方模型，也可以连接兼容 OpenAI Chat Completions API 的模型服务。DeepSeek 官方 API 兼容 OpenAI 格式，因此可以通过修改配置接入 DeepSeek。

配置调整：

```yaml
ai:
  chat-model:
    api-key: ${AI_API_KEY:${OPENAI_API_KEY:}}
    base-url: ${AI_BASE_URL:https://api.deepseek.com}
    model-name: ${AI_MODEL:deepseek-v4-flash}
```

说明：

- `AI_API_KEY` 是新的通用变量名，不再和 OpenAI 强绑定。
- `OPENAI_API_KEY` 保留为兼容旧配置。
- `AI_BASE_URL` 默认指向 DeepSeek API。
- `AI_MODEL` 默认使用 `deepseek-v4-flash`。
- 真实 API Key 不写入配置文件，不提交到 Git。

本地运行前设置：

```bash
export AI_API_KEY=你的_DeepSeek_API_Key
```


## Step 16：新增 AI 助教调用记录

这一步解决的问题：

V1 的 AI 助教接口已经能返回 `content`，但返回之后后端没有留下任何痕迹。这样会有几个问题：

- 用户看不到自己以前对某道题问过什么。
- 后端无法排查某次 AI 调用为什么失败。
- 后续无法统计 AI 功能使用情况，例如哪个助教类型最常用。
- 学习复盘时，只能看到代码，无法看到“发生过哪些模型调用”。

因此新增 `ai_assist_record` 表保存每一次调用。

### 新增数据库表

涉及文件：

- `src/main/resources/migrations/V0.0.11__Add_ai_assist_record_table.sql`

表名：

```sql
ai_assist_record
```

核心字段：

```sql
`id`            BIGINT        NOT NULL COMMENT 'AI助教调用记录ID',
`user_id`       BIGINT        NOT NULL COMMENT '用户ID',
`question_id`   BIGINT        NOT NULL COMMENT '题目ID',
`assist_type`   VARCHAR(64)   NOT NULL COMMENT '助教类型',
`user_input`    TEXT          NULL COMMENT '用户补充输入',
`content`       MEDIUMTEXT    NULL COMMENT 'AI返回内容',
`model_name`    VARCHAR(128)  NOT NULL COMMENT '模型名称',
`status`        VARCHAR(32)   NOT NULL COMMENT '调用状态：success成功 failed失败',
`error_message` VARCHAR(1024) NULL COMMENT '失败错误信息',
`latency_ms`    BIGINT        NULL COMMENT '调用耗时，单位毫秒'
```

为什么 `content` 用 `MEDIUMTEXT`：

- AI 返回可能比普通 VARCHAR 长很多。
- `TEXT` 最大约 64KB，部分题解可能不够。
- `MEDIUMTEXT` 能覆盖当前学习项目里的长回答场景。

为什么 `error_message` 限制 1024：

- 错误信息只用于排查，不应该无限保存。
- 第三方模型返回的错误有时会很长，限制长度可以避免异常日志撑爆字段。

为什么加索引：

```sql
CREATE INDEX `idx_aar_user_question_id`
    ON `ai_assist_record` (`user_id`, `question_id`, `id`);
```

这个索引用于查询“当前用户在某道题下的 AI 调用记录”，同时按 `id` 做游标分页。

### 新增实体

涉及文件：

- `src/main/java/com/chushi/aiinterview/entities/AiAssistRecord.java`

作用：

- Java 里对应 `ai_assist_record` 表。
- Mapper 插入记录时使用这个对象。

字段和数据库表基本一一对应。

### 新增 Mapper

涉及文件：

- `src/main/java/com/chushi/aiinterview/mappers/AiAssistRecordMapper.java`
- `src/main/resources/mappers/AiAssistRecordMapper.xml`

`insert` 使用注解：

```java
int insert(AiAssistRecord record);
```

列表查询使用 XML：

```java
List<AiAssistRecordVo> findRecordListByQuestionId(Long userId, Long questionId, Long cursor, Integer limit);
```

为什么插入用注解，查询用 XML：

- 项目里已有记录类 Mapper 也是这种风格。
- 插入 SQL 简单，注解可读性够。
- 查询 SQL 有分页、JOIN、字段别名，用 XML 更清楚。

### 新增 VO

涉及文件：

- `src/main/java/com/chushi/aiinterview/commons/vo/AiAssistRecordVo.java`
- `src/main/java/com/chushi/aiinterview/commons/vo/AiAssistRecordListVo.java`

`AiAssistRecordVo` 是单条记录返回给前端的结构。

`AiAssistRecordListVo` 保持和项目里其他列表接口一致：

```java
private List<AiAssistRecordVo> records;
```

### Service 如何记录成功和失败

涉及文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiQuestionAssistServiceImpl.java`

成功路径：

```java
var content = getChatModel().chat(prompt);
recordAiAssist(questionId, request, assistType, currentUser, content, RECORD_STATUS_SUCCESS, null, startNanos);
return new AiQuestionAssistVo(content);
```

失败路径：

```java
recordAiAssist(questionId, request, assistType, currentUser, null, RECORD_STATUS_FAILED, e.getMessage(), startNanos);
```

设计重点：

- `startNanos` 在调用模型前记录，用于计算 `latency_ms`。
- `BusinessException` 原样抛出，不改变原本业务错误语义。
- 普通异常统一包装成 `AI service call failed`。
- 记录保存本身如果失败，只写 warn 日志，不影响用户调用 AI 助教。

为什么记录保存失败不影响主流程：

AI 助教主目标是给用户返回答案。调用记录是辅助能力，不能因为记录表临时异常导致用户拿不到 AI 答案。

### 新增历史查询接口

涉及文件：

- `src/main/java/com/chushi/aiinterview/controller/AiQuestionAssistController.java`
- `src/main/java/com/chushi/aiinterview/services/AiQuestionAssistService.java`

接口：

```http
GET /api/ai/question/{questionId}/assist/records?last_id=&size=10
```

参数：

- `last_id`：游标，查比这个 ID 更早的记录。
- `size`：每页数量，范围 1 到 50。

为什么查询前还要调用：

```java
questionService.getQuestionById(questionId, currentUser);
```

原因是复用题目详情已有权限逻辑。用户不能看的题目，也不能看这道题对应的 AI 调用记录。

### 它和 Chat Memory 的区别

调用记录：

- 保存历史。
- 用于展示、排错、统计。
- 不参与下一次模型调用。

Chat Memory：

- 保存多轮上下文。
- 下一轮调用模型时会把历史消息带进去。
- 会影响模型回答。

当前实现是调用记录，不是 Chat Memory。

### 它和 RAG 的区别

RAG 通常包含：

- 文档切分。
- embedding 向量化。
- 向量库保存。
- 相似度召回。
- 把召回内容拼到 Prompt。

当前实现只是把模型调用结果保存到 MySQL，没有检索增强，所以不是 RAG。

### 验证

先直接运行编译：

```bash
./mvnw -q -DskipTests compile
```

结果失败，原因是终端默认 Java 不是 JDK 17。

再指定 JDK 17：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -DskipTests compile
```

结果：编译通过。


### Step 16 运行期验证结果

后端启动后，Flyway 输出：

```text
Migrating schema `interview` to version "0.0.11 - Add ai assist record table"
Successfully applied 1 migration to schema `interview`, now at version v0.0.11
```

这说明 migration 文件被识别，并且 `ai_assist_record` 表已经创建。

接口验证结果：

- 登录接口 HTTP 200。
- AI 助教接口 HTTP 200，`code=0`。
- AI 返回 `content` 长度为 326。
- 调用记录查询接口 HTTP 200，`code=0`。
- 最新调用记录状态为 `success`。
- 最新调用记录类型为 `key_points`。
- 最新调用记录里的 `content` 长度同样为 326。
- 最新调用记录耗时 `latency_ms=3078`。

验证时遇到的小问题：

项目的 JSON 请求字段是下划线风格，所以短信登录验证码字段要写：

```json
{
  "captcha_code": "123456"
}
```

如果写成 Java 字段名：

```json
{
  "captchaCode": "123456"
}
```

后端 DTO 中会绑定成 `captchaCode=null`，验证码校验失败。
