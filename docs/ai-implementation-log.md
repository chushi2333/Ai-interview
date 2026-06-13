# AI 模块实施记录

本文档用于记录 AI 模块从零接入过程中的每一步决策、改动和验证结果，方便后续复盘。

## 记录规则

每推进一个明确步骤，都记录以下内容：

- 时间：记录本次步骤发生的时间。
- 目标：这一步想解决什么问题。
- 原因：为什么现在做这一步。
- 改动：新增或修改了哪些内容。
- 涉及文件：列出主要文件。
- 验证：如何确认这一步有效。
- 结果：完成、失败或遗留问题。
- 下一步：下一步准备做什么。

## Step 0：恢复项目环境

时间：2026-06-02

目标：切换到 AI 相关分支，并启动项目依赖环境。

原因：当前工作要进入 AI 模块开发，需要基于完整业务分支和 Docker 开发环境继续。

改动：

- 切换分支到 `feature/ai-langchain4j`。
- 使用 `docker-compose.dev.yml` 启动开发依赖环境。

涉及文件：

- `docker-compose.dev.yml`
- `docs/docker-dev.md`

验证：

- 执行 `docker compose -f docker-compose.dev.yml ps`。
- MySQL、Redis、RabbitMQ、SeaweedFS、Elasticsearch 均为 healthy。
- Elasticvue 处于 running 状态。

结果：完成。

下一步：重新整理 AI 模块需求，明确 LangChain4j、RAG、Tool Calling、MCP 的学习和实现顺序。

## Step 1：重写 AI 模块需求分析

时间：2026-06-02

目标：把 AI 模块需求从功能堆叠改成“产品需求 + 学习路线 + 工程阶段”的结构。

原因：AI 模块会涉及 LangChain4j、Prompt、Chat Memory、Tool Calling、RAG、MCP。若不先拆清楚阶段，很容易一开始混用概念，导致实现失焦。

改动：

- 重写 `docs/ai-module-requirements.md`。
- 明确 V1 只做 AI 题解助教。
- 明确 V1 不做 RAG、Tool Calling、MCP、流式输出、多轮会话。
- 明确 RAG、Tool Calling、MCP 的使用时机和区别。
- 明确从零开始的实现顺序。

涉及文件：

- `docs/ai-module-requirements.md`

验证：

- 阅读文档确认包含目标定位、能力边界、业务场景、技术概念分层、阶段路线和推荐开发顺序。
- 使用 `git diff -- docs/ai-module-requirements.md` 确认只修改了需求文档。

结果：完成。

下一步：开始 V1 AI 题解助教。第一步只接 LangChain4j 基础模型调用和当前题目上下文。

## Step 2：准备接入 V1 AI 题解助教

时间：2026-06-02

目标：开始实现 V1，但先确认技术依赖、现有题目查询能力和代码落点。

原因：V1 的目标是打通最小闭环：`questionId -> 查询题目上下文 -> 构造 Prompt -> 调用模型 -> 返回文本`。实现前需要先确认 LangChain4j Spring Boot 接入方式，以及当前项目如何查询题目和处理权限。

计划改动：

- 确认 LangChain4j Spring Boot starter 依赖和配置项。
- 梳理 `QuestionService`、`QuestionMapper`、`QuestionVo` 的现有能力。
- 新增 AI 请求和响应 DTO。
- 新增 AI 助教 Service。
- 新增 AI Controller。
- 增加配置项和缺少 API Key 时的兜底。

涉及文件：

- `pom.xml`
- `src/main/resources/application-dev.yaml`
- `src/main/java/com/chushi/aiinterview/controller/*`
- `src/main/java/com/chushi/aiinterview/services/*`
- `src/main/java/com/chushi/aiinterview/commons/dto/*`
- `src/main/java/com/chushi/aiinterview/commons/vo/*`

验证计划：

- 使用 Maven 编译验证。
- 启动应用后调用 `POST /api/ai/question/{questionId}/assist`。
- 验证题目不存在、类型非法、API Key 缺失等错误路径。

结果：完成。

代码梳理结果：

- `QuestionService.getQuestionById(questionId, currentUser)` 已经能返回题目详情。
- 该方法会处理题目不存在、会员题权限、看题记录，并返回包含答案的 `QuestionVo`。
- `QuestionVo` 可直接提供 AI Prompt 所需的标题、内容、答案、标签、题库 ID 和题库标题。
- V1 AI 助教应复用 `QuestionService.getQuestionById`，不要直接调用 Mapper 绕过业务规则。
- Controller 风格使用 `BaseController.wrap(...)` 返回统一 `Response<T>`，权限注解使用 `@RequireRole`。

验证：

- 已读取 `QuestionService`、`QuestionServiceImpl`、`QuestionVo`、`QuestionController`。
- 确认 V1 查询题目上下文无需新增 Mapper。

下一步：新增 LangChain4j 依赖和 AI 配置，准备基础模型调用。


## Step 3：确定 V1 模型接入方式

时间：2026-06-02

目标：确定 LangChain4j 在 V1 中的接入方式和配置策略。

原因：V1 需要调用模型，但开发环境不一定总是配置 API Key。若直接依赖自动配置在启动时强制创建模型 Bean，可能导致没有 API Key 时整个应用启动失败，不利于本地开发和复盘。

决策：

- V1 使用 `langchain4j-open-ai` 基础依赖接入 OpenAI 兼容 ChatModel。
- 暂不使用自动配置强绑定模型 Bean。
- 新增项目自己的 `AiProperties` 读取 `ai.chat-model.*` 配置。
- 在 AI Service 调用时检查 API Key，缺失时返回明确业务错误。
- 模型对象懒加载，避免每次请求重复创建。

计划改动：

- 在 `pom.xml` 增加 LangChain4j 版本和依赖。
- 新增 `AiProperties`。
- 在配置文件中增加 AI 模型配置项。
- 新增 V1 AI 助教相关 DTO、VO、enum、service、controller。

涉及文件：

- `pom.xml`
- `src/main/resources/application.yaml`
- `src/main/resources/application-dev.yaml`
- `src/main/java/com/chushi/aiinterview/configurations/AiProperties.java`
- `src/main/java/com/chushi/aiinterview/commons/enums/AiQuestionAssistType.java`
- `src/main/java/com/chushi/aiinterview/commons/dto/AiQuestionAssistRequestDto.java`
- `src/main/java/com/chushi/aiinterview/commons/vo/AiQuestionAssistVo.java`
- `src/main/java/com/chushi/aiinterview/services/AiQuestionAssistService.java`
- `src/main/java/com/chushi/aiinterview/services/impl/AiQuestionAssistServiceImpl.java`
- `src/main/java/com/chushi/aiinterview/controller/AiQuestionAssistController.java`

验证计划：

- Maven 编译。
- 无 API Key 时接口返回明确配置错误。
- 有 API Key 时接口能基于题目上下文返回内容。

实际改动：

- `pom.xml` 增加 `langchain4j-open-ai` 依赖和 `spring-boot-starter-validation`。
- 新增 `AiProperties` 读取 AI 模型配置。
- `ApplicationConfiguration` 启用 `AiProperties`。
- 新增 `AiQuestionAssistType` 定义 V1 支持的助教类型。
- 新增 `AiQuestionAssistRequestDto` 和 `AiQuestionAssistVo`。
- 新增 `AiQuestionAssistService` 和实现类。
- 新增 `AiQuestionAssistController`，接口为 `POST /api/ai/question/{questionId}/assist`。
- `application.yaml` 和 `application-dev.yaml` 增加 `ai.chat-model.*` 配置。

结果：代码已写入，编译验证完成。

验证结果：

- 直接执行 `./mvnw -q -DskipTests compile` 失败，因为当前终端默认 Java 是 8。
- 项目要求 Java 17，且已有代码使用 text block、record、pattern matching for instanceof 等 Java 17 语法。
- 本机存在 JDK 17：`/usr/lib/jvm/java-17-openjdk-amd64`。
- 使用下面命令编译通过：

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH ./mvnw -q -DskipTests compile
```

补充文档：

- 新增 `docs/ai-v1-implementation-notes.md`，逐步解释 V1 的依赖、配置、DTO、VO、Service、Controller、Prompt 和编译验证。


## Step 4：调整 AI 配置默认值位置

时间：2026-06-02

目标：让 AI 模型配置更清晰，避免 Java 类和 YAML 同时维护默认值。

原因：`AiProperties` 中如果写默认值，同时 YAML 中也写默认值，会造成学习和维护上的歧义。配置项应该优先在配置文件中体现，Java 类只负责绑定结构。

改动：

- 移除 `AiProperties.ChatModelProperties` 中的业务默认值。
- 保留 `application.yaml` 和 `application-dev.yaml` 中的默认值。
- 更新 `docs/ai-v1-implementation-notes.md`，解释默认值为什么放在 YAML。

涉及文件：

- `src/main/java/com/chushi/aiinterview/configurations/AiProperties.java`
- `docs/ai-v1-implementation-notes.md`

验证：

- 使用 JDK 17 执行编译：

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH ./mvnw -q -DskipTests compile
```

结果：通过。


## Step 5：切换默认模型到 DeepSeek V4 Flash

时间：2026-06-12

目标：把 V1 AI 助教默认模型服务从 OpenAI 配置切换为 DeepSeek V4 Flash。

原因：当前使用的 LangChain4j `OpenAiChatModel` 支持 OpenAI 兼容 API。DeepSeek 官方 API 兼容 OpenAI 格式，因此无需改模型调用代码，只需要调整 base URL、模型名和 API Key 环境变量。

改动：

- `api-key` 改为优先读取 `AI_API_KEY`，并兼容 `OPENAI_API_KEY`。
- `base-url` 默认改为 `https://api.deepseek.com`。
- `model-name` 默认改为 `deepseek-v4-flash`。
- 更新 `docs/ai-v1-implementation-notes.md`，说明 DeepSeek Flash 配置方式。

涉及文件：

- `src/main/resources/application.yaml`
- `src/main/resources/application-dev.yaml`
- `docs/ai-v1-implementation-notes.md`

安全说明：

- 用户提供的 DeepSeek API Key 不写入代码、不写入配置文件、不提交到 Git。
- 本地运行时通过环境变量 `AI_API_KEY` 设置。

验证：

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH ./mvnw -q -DskipTests compile
```

结果：通过。


## Step 6：运行期验证 DeepSeek Flash AI 助教

时间：2026-06-12

目标：实际启动后端并调用 `POST /api/ai/question/{questionId}/assist`，确认 V1 AI 助教能通过 DeepSeek Flash 返回内容。

原因：前面只完成了编译验证，还需要验证运行期配置、数据库题目数据、权限拦截、题目上下文查询、Prompt 构造和模型调用链路。

验证计划：

1. 确认 Docker 依赖环境运行状态。
2. 确认后端是否已启动；若未启动则用 JDK 17 启动。
3. 从数据库或接口找一个真实 `questionId`。
4. 获取登录 token。
5. 调用 AI 助教接口测试正常路径。
6. 测试非法 `type` 错误路径。
7. 确认真实 API Key 不出现在终端输出和文档中。

安全说明：

- 运行期测试不会打印 API Key。
- 如果 key 被写入 `application-dev.yaml`，后续提交前必须移除或改回环境变量。

验证执行：

- Docker 依赖环境均为 healthy。
- 后端使用 JDK 17 启动成功，`/actuator/health` 返回 `UP`。
- 使用 Redis 写入测试短信验证码，通过 `/api/auth/login-via-sms` 获取 token。
- 使用真实题目 `81824214700527616` 调用：

```http
POST /api/ai/question/81824214700527616/assist
```

请求体：

```json
{
  "type": "simple_explain",
  "user_input": "我想快速理解这道题的核心考点"
}
```

验证结果：

- 登录接口返回 HTTP 200，成功获取 token。
- AI 助教接口返回 HTTP 200，`code=0`。
- DeepSeek Flash 返回了围绕 JDK/JRE/JVM 题目的解释内容。
- 非法 `type=bad_type` 返回 HTTP 400，错误信息为 `Unsupported AI assist type`。

结果：通过。


## Step 7：V1.1 AI 助教调用记录设计

时间：2026-06-12

目标：在 V1 AI 题解助教已经打通模型调用后，增加调用记录能力。

原因：V1 当前只返回 `content`，后端无法追踪用户何时、在哪道题、用哪个助教类型调用了 AI，也无法复盘模型调用失败、耗时和输出内容。调用记录不是 Chat Memory，不参与下一次模型上下文，只用于产品历史、工程排查和学习复盘。

设计边界：

- 保存 AI 助教调用记录。
- 不把历史记录拼回 Prompt。
- 不做多轮上下文。
- 不做 RAG。
- 不做 MCP。
- 不做缓存命中逻辑。

计划改动：

- 新增 Flyway 迁移 `ai_assist_record` 表。
- 新增实体 `AiAssistRecord`。
- 新增 Mapper 和 XML。
- 在 `AiQuestionAssistServiceImpl` 中记录成功和失败调用。
- 新增查询当前题目 AI 助教调用历史的接口。
- 更新 V1 实现笔记，解释“调用记录”和“Chat Memory”的区别。

涉及文件：

- `src/main/resources/migrations/*`
- `src/main/java/com/chushi/aiinterview/entities/*`
- `src/main/java/com/chushi/aiinterview/mappers/*`
- `src/main/resources/mappers/*`
- `src/main/java/com/chushi/aiinterview/services/impl/AiQuestionAssistServiceImpl.java`
- `src/main/java/com/chushi/aiinterview/controller/AiQuestionAssistController.java`
- `docs/ai-v1-implementation-notes.md`

验证计划：

- JDK 17 编译。
- 启动应用触发 Flyway 迁移。
- 调用 AI 助教正常路径后确认表中有成功记录。
- 调用非法或失败路径时确认可记录失败信息，且不影响业务错误返回。

实现结果：

- 新增 Flyway 迁移 `V0.0.11__Add_ai_assist_record_table.sql`。
- 新增 `ai_assist_record` 表，用于保存 AI 助教每一次调用。
- 新增实体 `AiAssistRecord`。
- 新增返回对象 `AiAssistRecordVo` 和 `AiAssistRecordListVo`。
- 新增 `AiAssistRecordMapper` 和 `AiAssistRecordMapper.xml`。
- `AiQuestionAssistServiceImpl` 在模型调用成功后记录 `success`，在模型调用异常后记录 `failed`。
- 新增查询接口：

```http
GET /api/ai/question/{questionId}/assist/records?last_id=&size=
```

记录字段说明：

- `user_id`：谁调用的。
- `question_id`：针对哪道题。
- `assist_type`：使用哪种助教类型，例如 `simple_explain`。
- `user_input`：用户补充输入。
- `content`：AI 返回内容，成功时保存。
- `model_name`：实际配置的模型名。
- `status`：`success` 或 `failed`。
- `error_message`：失败原因，最多保存 1024 字符。
- `latency_ms`：模型调用耗时，单位毫秒。
- `create_time` / `update_time`：记录时间。

实现注意点：

- 查询历史前仍然调用 `questionService.getQuestionById(questionId, currentUser)`，复用题目权限校验。
- 保存调用记录时包了一层 `try/catch`，记录失败只写日志，不影响 AI 助教接口主流程。
- 这一步仍然不是 Chat Memory，因为历史记录没有回填到下一次 Prompt。
- 这一步仍然不是 RAG，因为没有向量库、召回、重排，也没有把检索资料注入 Prompt。

验证：

第一次直接执行：

```bash
./mvnw -q -DskipTests compile
```

结果失败，原因是当前 shell 默认 Java 版本不是 17。项目使用了 Java 17 语法，Java 8 会把 text block、switch expression 等语法误报为编译错误。

随后指定 JDK 17 执行：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -DskipTests compile
```

结果：通过。


运行期验证：

- Docker 依赖环境均在运行，MySQL、Redis、RabbitMQ、Elasticsearch、SeaweedFS 为 healthy。
- Spring Boot 使用 dev profile 启动成功，端口 `8080`。
- Flyway 成功从 `0.0.10` 迁移到 `0.0.11`。
- MyBatis 成功解析 `AiAssistRecordMapper.xml`。
- MySQL 中确认 `ai_assist_record` 表和索引已创建。
- `/actuator/health` 返回 `UP`。
- 通过短信登录获取 token，注意该项目 JSON 字段使用下划线风格，验证码字段为 `captcha_code`。
- 调用 AI 助教接口：

```http
POST /api/ai/question/81824214700527616/assist
```

请求体：

```json
{
  "type": "key_points",
  "user_input": "请只提炼重点，方便复盘"
}
```

结果：

- AI 助教接口返回 HTTP 200，`code=0`。
- 返回 `content` 长度为 326。
- 查询调用记录接口返回 HTTP 200，`code=0`。
- 最新记录 `status=success`。
- 最新记录 `assist_type=key_points`。
- 最新记录 `content` 长度为 326。
- 最新记录 `latency_ms=3078`。

补充说明：

- 本次验证没有在终端或文档中输出 API Key。
- 本次验证没有输出模型完整回答正文，只记录长度和状态。


## Step 8：V2 Chat Memory 独立设计文档

时间：2026-06-13

目标：在动代码前，单独建立 V2 Chat Memory 设计文档，避免和 V1 单次 AI 助教实现笔记混在一起。

新增文档：

- `docs/ai-v2-chat-memory-design.md`

文档内容：

- V2 要解决的问题。
- Chat Memory、调用记录、RAG 的区别。
- 多轮对话产品流程。
- `ai_chat_session` 和 `ai_chat_message` 表结构设计。
- 创建会话、查询会话、发送消息、查询消息接口设计。
- Prompt 组装方式。
- 历史消息长度控制。
- LangChain4j 手动 Prompt 和 Chat Memory 两种实现方式对比。
- 后端实现步骤和验收标准。

当前结论：

- V2 先实现围绕题目的持久化多轮对话。
- 第一版先手动组装最近历史消息，跑通业务闭环。
- 后续再重构到 LangChain4j Chat Memory / AI Service。
- V2 暂不做 RAG、MCP、Tool Calling。

结果：设计文档已创建，暂未修改业务代码。


## Step 9：V2 Chat Memory 第一版实现

时间：2026-06-13

目标：根据 `docs/ai-v2-chat-memory-design.md` 实现围绕题目的多轮 AI 对话能力。

实现内容：

- 新增 Flyway 迁移 `V0.0.12__Add_ai_chat_tables.sql`。
- 新增 `ai_chat_session` 表，保存题目 AI 对话会话。
- 新增 `ai_chat_message` 表，保存 user / assistant 消息。
- 新增实体：
  - `AiChatSession`
  - `AiChatMessage`
- 新增 DTO：
  - `AiChatSessionCreateDto`
  - `AiChatMessageCreateDto`
- 新增 VO：
  - `AiChatSessionVo`
  - `AiChatSessionListVo`
  - `AiChatMessageVo`
  - `AiChatMessageListVo`
  - `AiChatMessageSendVo`
- 新增 Mapper 和 XML：
  - `AiChatSessionMapper`
  - `AiChatMessageMapper`
  - `AiChatSessionMapper.xml`
  - `AiChatMessageMapper.xml`
- 新增 `AiChatService` 和 `AiChatServiceImpl`。
- 新增 `AiChatController`。
- 新增 `AiChatModelProvider`，统一管理模型实例创建。
- 将 V1 的 `AiQuestionAssistServiceImpl` 切换到 `AiChatModelProvider`，避免重复创建模型逻辑。

新增接口：

```http
POST /api/ai/question/{questionId}/chat/sessions
GET  /api/ai/question/{questionId}/chat/sessions
POST /api/ai/chat/sessions/{sessionId}/messages
GET  /api/ai/chat/sessions/{sessionId}/messages
```

关键实现说明：

- 创建会话时复用 `questionService.getQuestionById(questionId, currentUser)` 校验题目权限。
- 发送消息时先查旧历史消息，再保存当前用户消息，避免当前问题同时出现在“历史消息”和“当前用户问题”两处。
- 第一版每次最多带最近 10 条成功消息进入 Prompt。
- AI 回复成功时保存 `assistant` 消息，状态为 `success`。
- AI 调用失败时保存一条 `assistant` 失败消息，状态为 `failed`，并继续按业务异常返回。
- 查询消息列表按 `id DESC` 游标分页，返回最新消息在前。

编译验证：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -DskipTests compile
```

结果：通过。

运行期验证：

- Docker 依赖环境均为 healthy。
- 后端使用 dev profile 启动成功。
- Flyway 成功从 `0.0.11` 迁移到 `0.0.12`。
- MyBatis 成功解析 `AiChatSessionMapper.xml` 和 `AiChatMessageMapper.xml`。
- MySQL 中确认 `ai_chat_session` 和 `ai_chat_message` 表及索引已创建。
- `/actuator/health` 返回 `UP`。
- 使用短信登录获取 token，验证码字段使用 `captcha_code`。

接口验证结果：

- 创建会话：HTTP 200，`code=0`。
- 第一轮发送消息：HTTP 200，`code=0`，assistant 消息 `status=success`，content 长度 90。
- 第二轮发送消息：HTTP 200，`code=0`，assistant 消息 `status=success`，content 长度 577。
- 查询消息列表：HTTP 200，`code=0`，共 4 条消息。
- 消息角色顺序：`assistant, user, assistant, user`。
- 查询会话列表：HTTP 200，`code=0`，共 1 条会话。

补充说明：

- 消息列表按 `id DESC` 返回，所以最新 assistant 消息在最前面。
- 本次验证没有输出 API Key。
- 本次验证没有输出完整模型回答正文，只记录状态和长度。


## Step 10：V2.1 消息列表返回时间正序

时间：2026-06-13

目标：让消息列表接口返回结果更适合前端聊天窗口展示。

背景：

V2 第一版的消息列表查询按 `id DESC` 返回，结果是最新消息在前，例如：

```text
assistant
user
assistant
user
```

这对数据库游标分页是合适的，但前端聊天窗口一般希望从旧到新展示：

```text
user
assistant
user
assistant
```

实现方式：

- Mapper 仍然按 `id DESC` 查询最新一页，保留游标分页效率。
- Service 层拿到这一页后使用 `Collections.reverse(messages)` 反转。
- Controller 返回给前端的是时间正序列表。

涉及文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

关键代码：

```java
var messages = new ArrayList<>(aiChatMessageMapper.findMessageListBySessionId(sessionId, currentUser.getId(), lastId, size));
Collections.reverse(messages);
return messages;
```

验证：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -DskipTests compile
```

结果：通过。

## Step 11：V2.2 AI 对话会话管理

时间：2026-06-13

目标：让 AI 对话会话具备基础管理能力，方便前端展示多个历史对话，也方便用户整理对话记录。

本次解决的问题：

- 会话只能创建和查询，不能重命名。
- 会话不能删除，历史对话会一直出现在列表里。
- 默认会话标题通常来自题目标题，多次追问后不容易区分。

### 1. 新增会话重命名 DTO

新增文件：

- `src/main/java/com/chushi/aiinterview/commons/dto/AiChatSessionUpdateDto.java`

作用：

- 接收前端修改会话标题的请求体。
- 使用 `@NotBlank` 保证标题不能为空。
- 使用 `@Size(max = 128)` 保证标题长度不超过数据库字段限制。

核心字段：

```java
@NotBlank(message = "title must not be blank")
@Size(max = 128, message = "title length must be less than 128")
private String title;
```

### 2. 新增软删除字段迁移

新增文件：

- `src/main/resources/migrations/V0.0.13__Alter_ai_chat_session_add_is_delete.sql`

作用：

- 给 `ai_chat_session` 增加 `is_delete` 字段。
- 删除会话时只把 `is_delete` 改成 `1`，不物理删除消息数据。
- 新增复合索引，保证按用户、题目、删除状态、ID 查询会话列表。

核心 SQL：

```sql
ALTER TABLE `ai_chat_session`
    ADD COLUMN `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0未删除 1已删除' AFTER `status`;

CREATE INDEX `idx_acs_user_question_delete_id`
    ON `ai_chat_session` (`user_id`, `question_id`, `is_delete`, `id`);
```

### 3. 会话实体增加删除标记

修改文件：

- `src/main/java/com/chushi/aiinterview/entities/AiChatSession.java`

新增字段：

```java
private Integer isDelete;
```

作用：

- 和数据库 `is_delete` 字段对应。
- 后续如果需要在业务层判断删除状态，可以直接从实体读取。

### 4. Mapper 增加重命名和软删除 SQL

修改文件：

- `src/main/java/com/chushi/aiinterview/mappers/AiChatSessionMapper.java`
- `src/main/resources/mappers/AiChatSessionMapper.xml`

新增方法：

```java
int updateTitle(Long id, Long userId, String title, LocalDateTime updateTime);

int softDelete(Long id, Long userId, LocalDateTime updateTime);
```

关键点：

- `findById` 增加 `AND is_delete = 0`。
- `updateTime` 增加 `AND is_delete = 0`。
- 会话列表查询增加 `AND acs.is_delete = 0`。
- 重命名和删除都带 `user_id` 条件，避免越权修改别人的会话。

### 5. Service 增加会话管理方法

修改文件：

- `src/main/java/com/chushi/aiinterview/services/AiChatService.java`
- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

新增方法：

```java
AiChatSessionVo updateSessionTitle(Long sessionId, AiChatSessionUpdateDto request, User currentUser);

void removeSession(Long sessionId, User currentUser);
```

重命名逻辑：

- 先通过 `getOwnedSession` 确认会话存在且属于当前用户。
- 截断标题到 128 字符以内。
- 更新 `title` 和 `update_time`。
- 返回更新后的 `AiChatSessionVo`。

删除逻辑：

- 先通过 `getOwnedSession` 做归属校验。
- 调用 `softDelete` 把 `is_delete` 改成 `1`。
- 不删除 `ai_chat_message` 数据。

### 6. 第一条消息自动生成会话标题

修改文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

逻辑位置：

- `sendMessage` 保存用户消息之后。
- 调用模型之前。

触发条件：

- 当前会话还没有历史消息。
- 当前标题仍是默认标题，例如：
  - `AI 对话`
  - 题目标题
  - `AI 追问：题目标题`

生成规则：

- 使用第一条用户追问作为标题来源。
- 去掉换行、制表符，并压缩连续空白。
- 加上 `追问：` 前缀。
- 最长保留 128 字符。

核心代码：

```java
var autoTitle = buildAutoSessionTitleIfNecessary(session, question, historyMessages, request.getContent());
if (autoTitle != null) {
    aiChatSessionMapper.updateTitle(sessionId, currentUser.getId(), autoTitle, now);
    session.setTitle(autoTitle);
} else {
    aiChatSessionMapper.updateTime(sessionId, now);
}
```

### 7. Controller 新增接口

修改文件：

- `src/main/java/com/chushi/aiinterview/controller/AiChatController.java`

新增接口：

```http
PUT /api/ai/chat/sessions/{sessionId}
DELETE /api/ai/chat/sessions/{sessionId}
```

作用：

- `PUT`：修改 AI 对话会话标题。
- `DELETE`：软删除 AI 对话会话。

权限：

- 和已有 AI 对话接口一致，要求 `USER`、`ADMIN` 或 `SUPER_ADMIN`。
- Service 层仍会二次校验会话归属。

### 8. 验证

执行命令：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -DskipTests compile
```

结果：通过。

