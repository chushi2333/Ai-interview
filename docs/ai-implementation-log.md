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

## Step 12：V2.3 短期上下文质量优化

时间：2026-06-13

目标：优化 AI 对话进入 Prompt 的最近历史消息，避免最近 10 条消息过长、空消息或失败消息影响模型回答。

这一步不是摘要记忆，也不是 RAG。它仍然属于短期记忆优化，为后续 V2.4 摘要记忆做基础。

### 1. 最近历史 SQL 增加空内容过滤

修改文件：

- `src/main/resources/mappers/AiChatMessageMapper.xml`

原本最近历史已经过滤：

```sql
AND status = 'success'
```

本次新增：

```sql
AND TRIM(content) != ''
```

作用：

- 只取成功消息。
- 不把空内容消息放进 Prompt。

### 2. Service 层增加二次防御过滤

修改文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

即使 SQL 已经过滤，Service 层仍然再次判断：

- `status` 必须是 `success`。
- `content` 必须有文本。

原因：

- Mapper 未来可能被复用或调整。
- Service 是 Prompt 构造的最后防线。

### 3. 单条历史消息长度限制

新增常量：

```java
private static final int MAX_HISTORY_MESSAGE_CONTENT_LENGTH = 800;
```

作用：

- 防止某一条 assistant 长回答把后续 Prompt 撑爆。
- 超过 800 字符的历史消息会截断，并追加 `...（已截断）`。

### 4. 总历史文本长度限制

新增常量：

```java
private static final int MAX_HISTORY_TEXT_LENGTH = 5000;
```

作用：

- 即使最近 10 条每条都很长，也控制最终进入 Prompt 的历史总长度。
- 如果超出限制，优先保留更接近当前问题的消息。
- 较早历史会被省略，并在 Prompt 中说明。

### 5. 历史消息格式升级

修改前：

```text
用户：...
助教：...
```

修改后：

```text
1. 用户：...
2. 助教：...
```

作用：

- 让模型更容易识别对话顺序。
- Prompt 中明确说明“序号越大越接近当前问题”。

### 6. 文本归一化

新增方法：

```java
normalizeHistoryContent(String content)
```

处理内容：

- 换行、制表符转为空格。
- 连续空白压缩成一个空格。
- 去掉首尾空白。

作用：

- 减少无意义格式占用 Prompt。
- 让最近历史更紧凑。

### 7. 验证

执行命令：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -DskipTests compile
```

结果：通过。

## Step 13：Prompt 合规修正，移除内部题目 ID

时间：2026-06-14

问题：

最新一次 AI 回复中输出了题目 ID。题目 ID 属于系统内部标识，不应该出现在面向用户的助教回答里。

原因：

`AiChatServiceImpl#buildPrompt` 的题目上下文里包含：

```text
题目 ID：%s
```

模型拿到这个字段后，可能会在回答中复述出来。

修改文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

修改内容：

- 从 Prompt 的当前题目上下文中移除 `题目 ID`。
- 从 `formatted(...)` 参数中移除 `question.getId()`。
- 在输出要求中新增约束：不要向用户暴露内部题目 ID、数据库 ID、会话 ID 等系统内部标识。

修改后的规则：

```text
# 当前题目上下文
题目标题
所属题库
难度
标签
```

验证：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -DskipTests compile
```

结果：通过。

## Step 14：V2.4 当前会话摘要记忆

时间：2026-06-14

目标：在短期历史裁剪基础上，增加当前会话的长期摘要记忆。长对话中，较早消息会被压缩进 `memory_summary`，最近消息继续作为短期记忆进入 Prompt。

这一步仍然不是 RAG。摘要只来自当前会话内部消息，不做向量检索，也不跨会话搜索。

### 1. 新增数据库字段

新增文件：

- `src/main/resources/migrations/V0.0.14__Alter_ai_chat_session_add_memory_summary.sql`

新增字段：

```sql
memory_summary MEDIUMTEXT NULL COMMENT 'AI对话长期记忆摘要'
summary_message_id BIGINT NULL COMMENT '摘要已覆盖到的消息ID'
```

字段含义：

- `memory_summary`：当前会话已经压缩出来的长期摘要。
- `summary_message_id`：摘要已经覆盖到哪一条消息，避免重复摘要同一批消息。

### 2. 会话实体增加摘要字段

修改文件：

- `src/main/java/com/chushi/aiinterview/entities/AiChatSession.java`

新增字段：

```java
private String memorySummary;
private Long summaryMessageId;
```

### 3. 会话 Mapper 支持查询和更新摘要

修改文件：

- `src/main/java/com/chushi/aiinterview/mappers/AiChatSessionMapper.java`

修改内容：

- `findById` 查询 `memory_summary` 和 `summary_message_id`。
- 新增 `updateMemorySummary`，用于保存最新长期摘要和覆盖到的消息 ID。

### 4. 消息 Mapper 支持摘要候选消息

修改文件：

- `src/main/java/com/chushi/aiinterview/mappers/AiChatMessageMapper.java`
- `src/main/resources/mappers/AiChatMessageMapper.xml`

新增能力：

- `countSuccessMessagesBySessionId`：统计当前会话成功且非空消息数量。
- `findSummaryMessagesBySessionId`：查询需要进入摘要的旧消息。

摘要候选消息规则：

- 必须是当前会话。
- 必须属于当前用户。
- `status = success`。
- `content` 非空。
- `id > summary_message_id`，避免重复摘要。
- `id < beforeMessageId`，保留最近 10 条作为短期记忆，不压入摘要。

### 5. Prompt 增加长期记忆摘要

修改文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

Prompt 从：

```text
当前题目上下文
最近对话历史
当前用户问题
```

变成：

```text
当前题目上下文
长期记忆摘要
最近对话历史
当前用户问题
```

如果当前会话还没有摘要，则长期记忆摘要为 `无`。

### 6. 发送消息后尝试刷新摘要

逻辑位置：

- 用户消息保存后。
- 模型正常回复后。
- assistant 消息保存后。
- 返回接口结果前尝试刷新摘要。

触发规则：

- 当前会话成功且非空消息数不少于 `12`。
- 最近 `10` 条消息保留给短期记忆。
- 只摘要最近 10 条之前、且还没有被 `summary_message_id` 覆盖的旧消息。
- 单次最多取 `30` 条旧消息做摘要。

失败策略：

- 摘要刷新用 `try/catch` 包住。
- 摘要失败只写 warn 日志。
- 不影响本轮用户消息和 assistant 回复。

### 7. 摘要 Prompt

新增摘要器 Prompt：

```text
已有长期摘要 + 新增待摘要对话 -> 更新后的长期记忆摘要
```

摘要要求：

- 使用中文。
- 只保留对后续学习和追问有帮助的信息。
- 保留用户暴露出的薄弱点、已经解释过的关键结论、尚未解决的问题。
- 不记录内部题目 ID、数据库 ID、会话 ID、消息 ID。
- 不逐字复述对话。
- 控制在 800 字以内。

### 8. 验证

执行命令：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -DskipTests compile
```

结果：通过。

## Step 15：V2.5 摘要触发频率优化

时间：2026-06-14

目标：降低长期摘要记忆的模型调用频率，避免超过 12 条消息后，每滑出少量旧消息就触发一次摘要。

背景：

V2.4 的摘要触发规则是：

- 成功且非空消息数达到 12 条。
- 最近 10 条保留为短期记忆。
- 最近 10 条之前、还没被摘要覆盖的旧消息进入摘要。

这样在长对话中可能比较频繁地触发摘要，因为每次有新消息进入，都会有旧消息滑出最近 10 条窗口。

### 修改文件

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

### 新增常量

```java
private static final int SUMMARY_MIN_SOURCE_MESSAGE_COUNT = 4;
```

含义：

至少累计 4 条未摘要旧消息，才调用模型刷新长期摘要。

### 新增判断

```java
if (summaryMessages.size() < SUMMARY_MIN_SOURCE_MESSAGE_COUNT) {
    return;
}
```

作用：

- 减少摘要模型调用次数。
- 降低成本和延迟。
- 让摘要以小批量方式更新，而不是每滑出 1 条消息就更新。

### 当前摘要触发规则

现在同时满足以下条件才会摘要：

1. 成功且非空消息数不少于 12 条。
2. 最近 10 条消息保留为短期记忆。
3. 最近 10 条之前存在未被 `summary_message_id` 覆盖的旧消息。
4. 未摘要旧消息数量不少于 4 条。

### 验证

执行命令：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -DskipTests compile
```

结果：通过。

## Step 16：V2.6 AI 对话记忆调试接口和自动化测试

时间：2026-06-14

目标：增加一个登录用户可访问的记忆调试接口，方便观察当前会话摘要记忆是否按预期变化，并补充自动化测试验证统计逻辑。

### 1. 新增记忆调试 VO

新增文件：

- `src/main/java/com/chushi/aiinterview/commons/vo/AiChatMemoryVo.java`

返回字段：

```java
private Long sessionId;
private String memorySummary;
private Long summaryMessageId;
private Integer successMessageCount;
private Integer recentMessageCount;
private Integer pendingSummaryMessageCount;
```

字段含义：

- `memorySummary`：当前会话长期摘要。
- `summaryMessageId`：摘要已经覆盖到哪条消息。
- `successMessageCount`：当前会话成功且非空消息总数。
- `recentMessageCount`：当前短期记忆窗口内的消息数量，最多 10。
- `pendingSummaryMessageCount`：最近 10 条之前，尚未被摘要覆盖的旧消息数量。

### 2. 新增接口

修改文件：

- `src/main/java/com/chushi/aiinterview/controller/AiChatController.java`
- `src/main/java/com/chushi/aiinterview/services/AiChatService.java`
- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

新增接口：

```http
GET /api/ai/chat/sessions/{sessionId}/memory
```

权限：

- 和 AI 对话接口一致，需要 `USER`、`ADMIN` 或 `SUPER_ADMIN`。
- Service 层复用 `getOwnedSession`，只能查看自己的会话记忆。

### 3. Mapper 增加待摘要消息统计

修改文件：

- `src/main/java/com/chushi/aiinterview/mappers/AiChatMessageMapper.java`

新增方法：

```java
int countSummaryMessagesBySessionId(Long sessionId, Long userId, Long summaryMessageId, Long beforeMessageId);
```

作用：

统计当前会话中，最近 10 条之前、且还没有被 `summary_message_id` 覆盖的旧消息数量。

### 4. 自动化测试

新增文件：

- `src/test/java/com/chushi/aiinterview/services/impl/AiChatServiceImplTest.java`

测试内容：

- 当最近 10 条窗口已满时，接口会统计待摘要旧消息数量。
- 当最近消息不足 10 条时，待摘要消息数量直接返回 0。

测试方式：

- 使用 Mockito mock Mapper。
- 不启动 Spring 容器。
- 不连接数据库。
- 不调用真实 AI 模型。

执行命令：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -Dtest=AiChatServiceImplTest test
```

结果：通过。

## Step 17：V2.7 真实模型联调验证

日期：2026-06-14。

这一步不是新增业务代码，而是用真实运行环境验证 V2.4 到 V2.6 的记忆逻辑是否真的生效。

### 1. 启动当前后端代码

因为本机 `8080` 上已经有一个后端服务在运行，为了避免影响原服务，这次使用临时端口 `18080` 启动当前分支代码：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--server.port=18080
```

启动时 Flyway 校验通过：

- 数据库：`interview`
- 当前迁移版本：`0.0.14`
- `V0.0.14__Alter_ai_chat_session_add_memory_summary.sql` 已生效

### 2. 登录接口字段问题

测试登录时发现验证码字段必须传：

```json
{
  "phone": "19518815269",
  "captcha_code": "验证码"
}
```

不能传 `captchaCode`。

原因是当前接口参数按 snake_case 接收。传 `captchaCode` 时，后端日志里验证码字段是 `null`，会导致登录失败。

### 3. 创建 AI 对话会话

测试题目：

```text
questionId = 82856969085390848
```

创建出的会话：

```text
sessionId = 92978813939486720
```

### 4. 连续发送 7 轮消息

这次使用真实模型发送多轮消息，目标是让成功消息数超过 12 条，触发长期摘要逻辑。

测试结果：

```text
turn=2 assistant_status=success content_len=1119 latency_ms=11182
turn=3 assistant_status=success content_len=389 latency_ms=3942
turn=4 assistant_status=success content_len=1566 latency_ms=18826
turn=5 assistant_status=success content_len=663 latency_ms=6718
turn=6 assistant_status=success content_len=1372 latency_ms=14721
turn=7 assistant_status=success content_len=830 latency_ms=6482
```

第一轮模型也成功返回了内容，但测试脚本第一次读取响应字段时按 camelCase 读取，和后端 snake_case 返回不一致，脚本打印中断。后续已改为兼容 snake_case 继续验证。

### 5. 验证长期摘要

调用调试接口：

```http
GET /api/ai/chat/sessions/92978813939486720/memory
```

接口返回的核心结果：

```text
summaryMessageId = 92979001206771712
successMessageCount = 14
recentMessageCount = 10
memorySummaryLength = 712
pendingSummaryMessageCount = 0
```

数据库也确认：

- `ai_chat_session.memory_summary` 已写入摘要。
- `ai_chat_session.summary_message_id` 已更新。
- 当前成功消息数是 14。
- 最近 10 条之前已经没有待摘要旧消息。

### 6. 本次结论

V2.4 到 V2.6 的核心链路验证通过：

- 用户消息和 AI 回复可以正常保存。
- 最近 10 条短期上下文可以继续参与 prompt。
- 长期摘要会在消息数量达到阈值后自动生成。
- 摘要结果会写回 `ai_chat_session`。
- `/memory` 调试接口能看到摘要状态。
- 临时启动的 `18080` 后端服务已停止。

## Step 18：V2.8 摘要触发策略改为 A 方案

日期：2026-06-14。

这一步调整的是长期摘要的触发频率。

之前 V2.5 为了节省模型调用，把摘要刷新条件设置成：最近 10 条之前的未摘要旧消息至少累计 4 条，才调用模型更新长期摘要。

现在确认使用 v4 flash，模型成本可以接受，因此改成 A 方案：

```text
只要有旧消息滑出最近 10 条短期窗口，就允许刷新长期摘要。
```

### 修改文件

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

### 修改内容

把摘要最小批次从 4 改成 1：

```java
// A 方案：只要有旧消息滑出最近窗口，就用低成本模型刷新摘要，让长期记忆更及时。
private static final int SUMMARY_MIN_SOURCE_MESSAGE_COUNT = 1;
```

### 这样做的效果

一轮用户提问通常会产生两条成功消息：

- 用户消息 `user`
- AI 回复 `assistant`

当对话超过最近 10 条窗口后，旧消息会滑出短期窗口。现在只要存在这种旧消息，就可以触发摘要更新。

优点：

- 长期记忆更新更及时。
- 后续回答更早拿到压缩后的历史上下文。
- 更适合学习阶段观察摘要如何变化。

代价：

- 长对话时摘要模型调用会更频繁。
- 每次摘要仍然是辅助链路，失败不会影响本轮聊天回复。

## Step 19：V2.9 记忆调试接口可观测性增强

日期：2026-06-14。

目标：让 `/memory` 接口不仅返回当前摘要内容，还直接告诉我们“当前是否满足摘要触发条件”和“为什么”。

这一步不改变聊天主流程，也不改变数据库结构，只增强调试接口返回值。

### 1. 修改记忆调试 VO

修改文件：

- `src/main/java/com/chushi/aiinterview/commons/vo/AiChatMemoryVo.java`

新增字段：

```java
private String summaryStrategy;
private Boolean summaryTriggerReady;
private String summaryTriggerReason;
private Integer summaryTriggerSuccessMessageCount;
private Integer summaryRecentMessageReserved;
private Integer summaryMinSourceMessageCount;
```

字段含义：

- `summaryStrategy`：当前摘要策略。现在是 `immediate`，表示有旧消息滑出最近窗口就可以摘要。
- `summaryTriggerReady`：当前状态是否已经达到摘要触发条件。
- `summaryTriggerReason`：解释为什么能触发或为什么不能触发。
- `summaryTriggerSuccessMessageCount`：成功消息数阈值，现在是 `12`。
- `summaryRecentMessageReserved`：短期记忆保留数量，现在是 `10`。
- `summaryMinSourceMessageCount`：最少待摘要旧消息数量，现在是 `1`。

### 2. Service 层增加触发判断

修改文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

新增常量：

```java
private static final String SUMMARY_STRATEGY_IMMEDIATE = "immediate";
```

新增方法：

```java
private boolean isSummaryTriggerReady(int successMessageCount, int recentMessageCount, int pendingSummaryMessageCount)
```

判断条件和真实摘要刷新逻辑保持一致：

```text
成功消息数 >= 12
最近消息数 >= 10
待摘要旧消息数 >= 1
```

新增方法：

```java
private String buildSummaryTriggerReason(int successMessageCount, int recentMessageCount, int pendingSummaryMessageCount)
```

作用：返回当前不能触发摘要的原因，或者说明已经达到触发条件。

### 3. 返回示例

```json
{
  "sessionId": 1,
  "memorySummary": "...",
  "summaryMessageId": 20,
  "successMessageCount": 16,
  "recentMessageCount": 10,
  "pendingSummaryMessageCount": 1,
  "summaryStrategy": "immediate",
  "summaryTriggerReady": true,
  "summaryTriggerReason": "已达到摘要触发条件，下一次发送消息后可刷新长期摘要",
  "summaryTriggerSuccessMessageCount": 12,
  "summaryRecentMessageReserved": 10,
  "summaryMinSourceMessageCount": 1
}
```

### 4. 自动化测试

修改文件：

- `src/test/java/com/chushi/aiinterview/services/impl/AiChatServiceImplTest.java`

新增断言：

- 最近窗口已满、有待摘要旧消息时，`summaryTriggerReady = true`。
- 最近窗口未满时，`summaryTriggerReady = false`。
- 返回当前策略和三个阈值。
- 返回触发原因文案。

执行命令：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -Dtest=AiChatServiceImplTest test
```

结果：通过。

## Step 20：V3.1 用户级长期记忆基础结构

日期：2026-06-15。

目标：新增用户级长期记忆的数据结构和查询接口。

这一步不调用 AI，也不自动更新用户记忆。先把用户长期记忆这层数据打通，后续 V3.2 再把 session 摘要合并进来。

### 1. 新增数据库表

新增文件：

- `src/main/resources/migrations/V0.0.15__Add_ai_user_memory_table.sql`

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

设计规则：

- `user_id` 唯一，一位用户只维护一份长期学习记忆。
- `memory_summary` 保存用户级学习画像摘要。
- `source_session_count` 记录已经合并过多少个会话摘要。
- `last_source_session_id` 为后续 V3.2 记录最近来源会话做准备。

### 2. 新增实体和 VO

新增文件：

- `src/main/java/com/chushi/aiinterview/entities/AiUserMemory.java`
- `src/main/java/com/chushi/aiinterview/commons/vo/AiUserMemoryVo.java`

`AiUserMemoryVo` 返回字段：

```java
private Boolean hasMemory;
private String memorySummary;
private Integer sourceSessionCount;
private Long lastSourceSessionId;
private LocalDateTime createTime;
private LocalDateTime updateTime;
```

`hasMemory` 用来区分用户确实没有记忆，还是记忆内容为空。

### 3. 新增 Mapper

新增文件：

- `src/main/java/com/chushi/aiinterview/mappers/AiUserMemoryMapper.java`

当前支持：

```java
Optional<AiUserMemory> findByUserId(Long userId);
int insert(AiUserMemory memory);
int updateByUserId(...);
```

V3.1 查询接口只用 `findByUserId`。

`insert` 和 `updateByUserId` 是给 V3.2 自动合并用户记忆预留的。

### 4. 新增查询接口

修改文件：

- `src/main/java/com/chushi/aiinterview/controller/AiChatController.java`
- `src/main/java/com/chushi/aiinterview/services/AiChatService.java`
- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

新增接口：

```http
GET /api/ai/user-memory
```

权限：

- `USER`
- `ADMIN`
- `SUPER_ADMIN`

接口只查询当前登录用户自己的长期记忆。

如果还没有记忆，返回 `hasMemory=false` 和 `sourceSessionCount=0`，不会自动创建空记录。

### 5. 自动化测试

修改文件：

- `src/test/java/com/chushi/aiinterview/services/impl/AiChatServiceImplTest.java`

新增测试：

- 用户没有长期记忆时，返回空状态。
- 用户已有长期记忆时，返回已有摘要、来源会话数量和时间字段。

## Step 21：V3.2 基于 session 摘要合并用户长期记忆

日期：2026-06-15。

目标：在当前 session 摘要更新成功后，把这份 session 摘要继续合并进用户级长期记忆。

这一步开始调用模型生成用户级学习画像，但不是每条消息都调用。它复用已经压缩过的 session 摘要，降低 token 和调用成本。

### 1. 修改 session 摘要刷新入口

修改文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

原来调用：

```java
tryRefreshMemorySummary(session, currentUser);
```

现在改成：

```java
tryRefreshMemorySummary(session, question, currentUser);
```

原因：

用户长期记忆合并时需要题目信息，例如标题、题库、难度、标签。

### 2. session 摘要成功后触发用户记忆合并

在 `tryRefreshMemorySummary` 中，只有当前 session 摘要写入成功后，才调用：

```java
tryRefreshUserMemory(session, question, currentUser, limitedSummary);
```

这保证用户长期记忆的输入不是原始聊天消息，而是已经压缩后的 session 摘要。

### 3. 新增用户记忆合并方法

新增方法：

```java
private void tryRefreshUserMemory(AiChatSession session, QuestionVo question, User currentUser, String sessionSummary)
```

处理流程：

1. 如果本次 session 摘要为空，直接返回。
2. 查询当前用户已有的 `ai_user_memory`。
3. 构造用户记忆 Prompt。
4. 调用模型生成新的用户长期记忆。
5. 如果用户记忆已存在，更新 `memory_summary`。
6. 如果用户记忆不存在，插入一条新记录。

### 4. 新增用户记忆 Prompt

新增方法：

```java
private String buildUserMemoryPrompt(String currentUserMemory, String sessionSummary, QuestionVo question)
```

Prompt 输入：

```text
已有用户长期记忆
本次会话摘要
本次会话题目信息
```

Prompt 要求：

- 使用中文。
- 只记录对后续面试学习有长期价值的信息。
- 保留用户反复暴露的薄弱点、偏好的解释方式、尚未解决的问题。
- 不记录手机号、邮箱、密钥、验证码等隐私或敏感信息。
- 不记录内部题目 ID、数据库 ID、会话 ID、消息 ID。
- 不逐字复述本次会话摘要。
- 控制在 1000 字以内。

### 5. source_session_count 更新规则

如果当前用户还没有长期记忆：

```text
source_session_count = 1
last_source_session_id = 当前 sessionId
```

如果当前用户已有长期记忆：

- 当 `last_source_session_id` 等于当前 sessionId，不增加 `source_session_count`。
- 当来源 session 变化时，`source_session_count + 1`。

这样避免同一个 session 多次刷新摘要时重复计数。

### 6. 失败策略

用户长期记忆合并被 `try/catch` 包住。

如果合并失败：

- 只写 warn 日志：`AiUserMemoryRefreshException`。
- 不影响本轮聊天回复。
- 不影响当前 session 摘要写入。

### 7. 当前验证状态

执行测试命令时，编译被一个非 V3.2 文件阻塞：

```text
src/main/java/com/chushi/aiinterview/publishers/ESMessagePublisher.java:39: <identifier> expected
```

原因是该文件末尾存在单独一行：

```java
HashMap
```

这个文件不是 V3.2 本次修改目标。需要先清理这个语法错误后，才能继续运行：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -Dtest=AiChatServiceImplTest test
```

## Step 22：V3 文档复盘结构补充

日期：2026-06-15。

问题：V3 文档原来偏设计说明，没有足够明确地说明每一步“做什么、为什么、怎么做、改哪里、怎么验证”。

本次补充文件：

- `docs/ai-v3-user-memory-design.md`

新的文档结构：

```text
V3 做什么
V3 不做什么
V3.1 做什么：先搭用户记忆基础结构
  - 做什么
  - 为什么先这么做
  - 怎么做
  - 改了哪里
  - 怎么验证
V3.2 做什么：用 session 摘要更新用户长期记忆
  - 做什么
  - 为什么用 session 摘要来更新
  - 怎么做
  - 改了哪里
  - Prompt 约束
  - source_session_count 怎么算
  - 失败怎么办
  - 怎么验证
```

复盘时重点看：

- V3.1：理解 `ai_user_memory` 这张表为什么存在。
- V3.1：理解 `GET /api/ai/user-memory` 为什么只是查询，不自动创建空记录。
- V3.2：理解用户长期记忆为什么不是每条消息更新，而是基于 session 摘要更新。
- V3.2：理解 `tryRefreshUserMemory` 为什么放在 session 摘要写入成功之后。
- V3.2：理解用户记忆失败为什么不影响聊天主流程。

## Step 23：清理编译阻塞并验证 V3.2

日期：2026-06-15。

目标：清理之前阻塞 Maven 编译的语法错误，并重新验证 V3.2 代码。

### 1. 问题

之前运行测试时，编译失败：

```text
src/main/java/com/chushi/aiinterview/publishers/ESMessagePublisher.java:39: <identifier> expected
```

原因是文件末尾存在一行孤立的：

```java
HashMap
```

这行不是合法 Java 语句，会导致整个项目无法编译。

### 2. 处理结果

检查 `ESMessagePublisher.java` 末尾后，确认孤立的 `HashMap` 已经不存在，文件现在能正常编译。

### 3. 验证命令

执行 AI 服务单元测试：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -Dtest=AiChatServiceImplTest test
```

结果：通过。

执行整体编译：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw -q -DskipTests compile
```

结果：通过。

### 4. 当前结论

V3.1 和 V3.2 当前代码已经通过单元测试和整体编译。

下一步可以进入 V3.3：把 `ai_user_memory.memory_summary` 读入聊天 Prompt，让用户长期记忆真正参与 AI 回复。

## Step 24：V3.3 用户长期记忆进入聊天 Prompt

日期：2026-06-15。

目标：让 `ai_user_memory.memory_summary` 真正参与 AI 聊天回答。

### 1. 为什么做这一步

V3.1 建了用户记忆表和查询接口。

V3.2 在 session 摘要更新成功后，会更新用户长期记忆。

但是如果聊天 Prompt 不读取用户长期记忆，这份记忆只存在数据库里，不会影响 AI 回答。

所以 V3.3 要把用户长期记忆放进聊天 Prompt。

### 2. 修改文件

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`
- `docs/ai-v3-user-memory-design.md`

### 3. 发送消息时读取用户长期记忆

在 `sendMessage` 调用模型前新增查询：

```java
var userMemorySummary = aiUserMemoryMapper.findByUserId(currentUser.getId())
        .map(AiUserMemory::getMemorySummary)
        .orElse(null);
```

这一步只读数据库，不额外调用模型。

### 4. Prompt 结构升级

修改前：

```text
当前题目上下文
当前会话长期摘要
最近对话历史
当前用户问题
```

修改后：

```text
当前题目上下文
用户长期学习记忆
当前会话长期摘要
最近对话历史
当前用户问题
```

### 5. buildPrompt 参数变化

从：

```java
buildPrompt(question, session.getMemorySummary(), historyMessages, request.getContent())
```

改成：

```java
buildPrompt(question, userMemorySummary, session.getMemorySummary(), historyMessages, request.getContent())
```

这样用户级长期记忆和当前 session 摘要是两层独立记忆，不会混在同一个参数里。

## Step 25：V3.4 用户长期记忆调试信息增强

日期：2026-06-15。

目标：增强 `GET /api/ai/user-memory` 返回值，让它能直接展示用户长期记忆当前是否进入 Prompt、更新策略和最大长度。

### 1. 新增返回字段

修改文件：

- `src/main/java/com/chushi/aiinterview/commons/vo/AiUserMemoryVo.java`

新增字段：

```java
private Boolean promptEnabled;
private String updateStrategy;
private Integer maxMemoryLength;
```

字段含义：

- `promptEnabled`：是否进入聊天 Prompt。当前为 `true`。
- `updateStrategy`：更新策略。当前为 `session_summary`。
- `maxMemoryLength`：用户长期记忆最大长度。当前为 `3000`。

### 2. Service 返回策略字段

修改文件：

- `src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

新增常量：

```java
private static final String USER_MEMORY_UPDATE_STRATEGY_SESSION_SUMMARY = "session_summary";
```

`getCurrentUserMemory` 在用户有记忆和没有记忆时，都会返回：

```text
promptEnabled = true
updateStrategy = session_summary
maxMemoryLength = 3000
```

原因：

即使当前用户还没有记忆，系统策略也是“有记忆后会进入 Prompt，并且基于 session 摘要更新”。

### 3. 测试

修改文件：

- `src/test/java/com/chushi/aiinterview/services/impl/AiChatServiceImplTest.java`

新增断言：

- 空记忆状态返回策略字段。
- 已有记忆状态返回策略字段。

## Step 26：V3.5 真实接口联调验证用户长期记忆

日期：2026-06-15。

目标：用真实后端、真实数据库和真实模型调用，验证 V3 用户长期记忆链路是否跑通。

### 1. 启动当前后端

使用临时端口启动当前分支代码：

```bash
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--server.port=18080
```

启动结果：

```text
Tomcat started on port 18080
```

Flyway 结果：

```text
Current version of schema interview: 0.0.14
Migrating schema interview to version 0.0.15 - Add ai user memory table
Successfully applied 1 migration, now at version v0.0.15
```

### 2. 登录并查看初始用户记忆

通过短信验证码登录测试用户。

登录前调用：

```http
GET /api/ai/user-memory
```

返回核心状态：

```json
{
  "hasMemory": false,
  "sourceSessionCount": 0,
  "promptEnabled": true,
  "updateStrategy": "session_summary",
  "maxMemoryLength": 3000
}
```

说明当前用户还没有长期记忆，但系统策略已经启用：有记忆后会进入 Prompt，且基于 session 摘要更新。

### 3. 创建 AI 对话会话

测试题目：

```text
questionId = 82856969085390848
```

创建会话：

```text
sessionId = 93371710622928896
```

### 4. 连续发送 7 轮消息

7 轮消息全部成功返回：

```text
turn=1 assistant_status=success content_len=541 latency_ms=7823
turn=2 assistant_status=success content_len=720 latency_ms=7929
turn=3 assistant_status=success content_len=807 latency_ms=13778
turn=4 assistant_status=success content_len=891 latency_ms=12450
turn=5 assistant_status=success content_len=844 latency_ms=14091
turn=6 assistant_status=success content_len=948 latency_ms=15937
turn=7 assistant_status=success content_len=982 latency_ms=13484
```

第 6、7 轮之后，当前 session 摘要和用户长期记忆都被触发更新。

### 5. 验证当前 session 记忆

调用：

```http
GET /api/ai/chat/sessions/93371710622928896/memory
```

返回核心状态：

```json
{
  "summaryMessageId": 93371777077481472,
  "successMessageCount": 14,
  "recentMessageCount": 10,
  "pendingSummaryMessageCount": 0,
  "memorySummaryLength": 886,
  "summaryTriggerReady": false
}
```

说明：

- 当前 session 已经有长期摘要。
- 最近 10 条仍作为短期记忆保留。
- 最近 10 条之前的旧消息已经被摘要覆盖。

### 6. 验证用户长期记忆

调用：

```http
GET /api/ai/user-memory
```

返回核心状态：

```json
{
  "hasMemory": true,
  "sourceSessionCount": 1,
  "lastSourceSessionId": 93371710622928896,
  "memorySummaryLength": 1538,
  "promptEnabled": true,
  "updateStrategy": "session_summary",
  "maxMemoryLength": 3000
}
```

说明：

- 用户长期记忆已经生成。
- 来源会话是本次测试 session。
- 用户长期记忆已启用进入 Prompt。

### 7. 数据库确认

执行只读 SQL：

```sql
SELECT id, user_id, source_session_count, last_source_session_id, CHAR_LENGTH(memory_summary) AS memory_len
FROM ai_user_memory
ORDER BY update_time DESC
LIMIT 3;
```

结果核心字段：

```text
source_session_count = 1
last_source_session_id = 93371710622928896
memory_len = 1538
```

### 8. 本次结论

V3.1 到 V3.4 的链路真实验证通过：

```text
session 摘要生成成功
-> 用户长期记忆生成成功
-> /api/ai/user-memory 可查询
-> 用户长期记忆策略字段正确返回
-> 后续聊天 Prompt 会读取用户长期记忆
```

