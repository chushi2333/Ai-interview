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
    api-key: ${OPENAI_API_KEY:}
    base-url: ${AI_BASE_URL:https://api.openai.com/v1}
    model-name: ${AI_MODEL:gpt-4o-mini}
    temperature: ${AI_TEMPERATURE:0.2}
    timeout: ${AI_TIMEOUT:60s}
    log-requests: ${AI_LOG_REQUESTS:false}
    log-responses: ${AI_LOG_RESPONSES:false}
```

每个配置项的含义：

- `api-key`：模型服务的 API Key，从环境变量 `OPENAI_API_KEY` 读取，默认空。
- `base-url`：模型服务地址，默认 OpenAI 地址，也可以换成兼容 OpenAI API 的代理或其他服务。
- `model-name`：模型名称，默认 `gpt-4o-mini`。
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
model-name: ${AI_MODEL:gpt-4o-mini}
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
