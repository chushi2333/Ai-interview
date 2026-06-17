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

## Step 27：V4 RAG 需求分析，确定 pgvector 技术路线

日期：2026-06-15。

目标：进入新一轮需求分析，规划 V4 RAG，不直接写业务代码。

### 1. 技术路线

确定使用：

```text
PostgreSQL + pgvector
```

不使用：

```text
MySQL 存向量
Elasticsearch dense_vector
专门向量数据库
```

原因：

- 用户以前使用过 pgvector。
- pgvector 适合第一版 RAG 学习和落地。
- PostgreSQL 可以同时保存 chunk 元数据和 embedding。

### 2. 第一版范围

V4 第一版只做：

```text
题目知识 RAG
```

数据来源：

- 题目标题。
- 题目内容。
- 参考答案。
- 标签。
- 难度。
- 所属题库。

暂时不做：

- 用户长期记忆 RAG。
- 历史 AI 对话 RAG。
- 自动全量索引。
- 多向量库适配。

### 3. 新增文档

新增文件：

- `docs/ai-v4-rag-requirements.md`

文档内容包括：

- V4 做什么。
- V4 不做什么。
- 为什么用 PostgreSQL + pgvector。
- 第一版 RAG 范围。
- 索引流程。
- 检索流程。
- 表结构草案。
- Docker 环境需求。
- embedding 配置草案。
- 接口设计草案。
- Prompt 接入位置。
- chunk 切分策略。
- 成本控制。
- 风险点。
- 开发步骤。
- 验收标准。

### 4. 下一步

下一步进入 V4.1：环境和依赖。

要做：

1. Docker 增加 PostgreSQL + pgvector。
2. Spring Boot 增加 PostgreSQL JDBC 依赖。
3. 增加 RAG 数据源配置。
4. 验证 pgvector 容器可启动。

## Step 28：V4.1 增加 pgvector 环境和 RAG 基础配置

日期：2026-06-15。

目标：先把 RAG 的环境层准备好，不直接进入表结构、索引接口和聊天接入。

### 1. 这一步做什么

本步骤完成 V4.1：

1. Docker 增加 PostgreSQL + pgvector 容器。
2. Spring Boot 增加 PostgreSQL JDBC 驱动。
3. 增加 RAG 向量库连接配置。
4. 增加 embedding 模型配置项。

这一步只做环境和配置，不做：

- `ai_rag_chunk` 表。
- RAG Mapper。
- embedding 调用封装。
- 题目索引接口。
- 聊天 Prompt 接入 RAG。

这些会放到后续 V4.2 之后逐步做。

### 2. 为什么先做环境

RAG 的核心链路是：

```text
数据源 -> chunk 切分 -> embedding -> pgvector 保存 -> query embedding -> 相似度检索 -> Prompt 注入
```

如果没有 pgvector 容器和 PostgreSQL 驱动，后面的建表、写入向量、相似度检索都无法验证。

所以第一步先保证基础设施存在。

### 3. 改动位置

#### 3.1 `docker-compose.dev.yml`

新增服务：

```yaml
postgres:
  image: pgvector/pgvector:pg16
  container_name: ai-interview-postgres
  environment:
    POSTGRES_DB: ai_interview_rag
    POSTGRES_USER: postgres
    POSTGRES_PASSWORD: postgres
```

作用：启动一个带 pgvector 扩展能力的 PostgreSQL。

新增 volume：

```yaml
postgres_data:
```

作用：持久化 PostgreSQL 数据。

#### 3.2 `pom.xml`

新增依赖：

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

作用：让后端后续可以连接 PostgreSQL。

当前主业务库仍然是 MySQL，这个依赖只是为 RAG 向量库准备。

#### 3.3 `application.yaml`

新增 embedding 模型配置：

```yaml
ai:
  embedding-model:
    api-key: ${AI_EMBEDDING_API_KEY:${AI_API_KEY:${OPENAI_API_KEY:}}}
    base-url: ${AI_EMBEDDING_BASE_URL:https://api.openai.com/v1}
    model-name: ${AI_EMBEDDING_MODEL:text-embedding-3-small}
    dimension: ${AI_EMBEDDING_DIMENSION:1536}
```

作用：后续把题目文本、用户问题转成向量。

新增 RAG 数据源配置：

```yaml
rag:
  datasource:
    url: ${RAG_DATASOURCE_URL:jdbc:postgresql://localhost:5433/ai_interview_rag}
    username: ${RAG_DATASOURCE_USERNAME:postgres}
    password: ${RAG_DATASOURCE_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
```

作用：单独配置 pgvector 数据库，不影响 MySQL 主业务库。

#### 3.4 `AiProperties.java`

新增 `EmbeddingModelProperties`。

作用：让 `ai.embedding-model.*` 可以被 Spring Boot 绑定成 Java 配置对象。

#### 3.5 `RagProperties.java`

新增 RAG 配置类。

作用：让 `rag.datasource.*` 可以被 Spring Boot 绑定成 Java 配置对象。

#### 3.6 `ApplicationConfiguration.java`

注册：

```java
@EnableConfigurationProperties({AiProperties.class, RagProperties.class})
```

作用：让 `AiProperties` 和 `RagProperties` 都能被 Spring 容器识别。

### 4. 当前配置关系

当前项目变成两个数据库配置：

```text
MySQL
  -> spring.datasource
  -> 主业务数据：用户、题库、AI 对话、记忆摘要

PostgreSQL + pgvector
  -> rag.datasource
  -> RAG chunk 和 embedding 向量
```

注意：现在只是加了配置，还没有创建真正的 RAG DataSource Bean。

原因是 V4.1 只准备环境，V4.2 建表和 Mapper 时再正式接入 PostgreSQL 操作逻辑。

### 5. 怎么验证

后续验证分两层：

1. Docker 层：

```bash
docker compose -f docker-compose.dev.yml up -d postgres
```

然后检查：

```bash
docker exec ai-interview-postgres pg_isready -U postgres -d ai_interview_rag
```

2. 后端编译层：

```bash
./mvnw -q -DskipTests compile
```

### 6. 下一步

下一步进入 V4.2：表结构和 Mapper。

要做：

1. 创建 `ai_rag_chunk` 表。
2. 开启 pgvector extension。
3. 创建 RAG chunk 实体。
4. 创建 Mapper。
5. 支持插入 chunk、按 questionId 删除 chunk、相似度检索 topK。

## Step 29：V4.2 创建 RAG 表结构、Mapper 和 PostgreSQL 独立访问层

日期：2026-06-15。

目标：在 V4.1 环境基础上，补齐 RAG chunk 的数据库表、Java 实体、Mapper，以及 PostgreSQL 独立 MyBatis/Flyway 配置。

### 1. 这一步做什么

本步骤完成 V4.2：

1. 创建 `ai_rag_chunk` 表结构脚本。
2. 新增 `AiRagChunk` 实体。
3. 新增 `AiRagChunkSearchVo` 查询返回对象。
4. 新增 `AiRagChunkMapper`。
5. 新增 `AiRagChunkMapper.xml`。
6. 新增 RAG PostgreSQL 独立 MyBatis 配置。
7. 新增 RAG PostgreSQL 独立 Flyway 配置。

这一步仍然不做 embedding 模型调用。

也就是说，现在验证的是：

```text
pgvector 能保存向量字段
pgvector 能执行相似度查询
后端能识别 RAG Mapper
Spring 能同时启动 MySQL 和 PostgreSQL 两套数据源
```

不是：

```text
题目文本 -> embedding 模型 -> 向量
```

真正的 embedding 模型封装放到 V4.3。

### 2. 为什么要单独做 RAG 数据源

项目原本只有一个主数据源：

```text
spring.datasource -> MySQL
```

MySQL 保存主业务数据：用户、题库、对话、长期记忆。

RAG 需要 PostgreSQL + pgvector，所以不能把 RAG SQL 混进主 MySQL 数据源。

因此新增第二套数据访问层：

```text
rag.datasource -> PostgreSQL + pgvector
```

并且把 RAG Mapper 放在单独包下：

```text
com.chushi.aiinterview.rag.mappers
```

这样可以避免 `AiRagChunkMapper` 被主 MySQL 的 MyBatis 扫描到。

### 3. 改动位置

#### 3.1 `src/main/resources/rag-migrations/V0.0.1__Create_ai_rag_chunk_table.sql`

新增 RAG 建表脚本。

核心内容：

```sql
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
```

字段说明：

- `question_id`：来源题目 ID。
- `chunk_index`：同一题目下第几个 chunk。
- `source_type`：数据来源类型，第一版先用 `question`。
- `title`：chunk 标题，便于调试和展示。
- `content`：真正进入 Prompt 的文本内容。
- `embedding`：pgvector 向量字段，当前维度为 `1536`。
- `metadata`：扩展元数据，例如难度、标签、题库信息。

新增索引：

```sql
CREATE INDEX IF NOT EXISTS idx_ai_rag_chunk_question_id
    ON ai_rag_chunk (question_id);

CREATE INDEX IF NOT EXISTS idx_ai_rag_chunk_embedding
    ON ai_rag_chunk USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
```

`question_id` 索引用于按题目删除和重建索引。

`ivfflat` 索引用于后续向量相似度检索。

### 3.2 `AiRagChunk.java`

新增实体类。

注意：

```java
private String embedding;
```

这里暂时用 `String` 承载向量，例如：

```text
[0.1,0.2,0.3,...]
```

原因是 Java 侧先不引入 pgvector 专用类型，Mapper 写入 PostgreSQL 时用：

```sql
#{embedding}::vector
```

把字符串转成 pgvector 类型。

### 3.3 `AiRagChunkSearchVo.java`

新增检索结果 VO。

包含：

- chunk 基本信息。
- `distance` 相似度距离。

`distance` 越小，表示越相似。

### 3.4 `AiRagChunkMapper.java`

新增 Mapper 接口，支持：

```java
int insert(AiRagChunk chunk);

int deleteByQuestionId(Long questionId);

int countByQuestionId(Long questionId);

List<AiRagChunkSearchVo> searchTopK(String embedding, Integer limit);
```

当前只准备底层能力，不直接暴露 Controller。

### 3.5 `AiRagChunkMapper.xml`

新增 RAG SQL。

插入时：

```sql
#{embedding}::vector
#{metadata}::jsonb
```

检索时：

```sql
embedding <=> #{embedding}::vector
```

这里 `<=>` 是 pgvector 的 cosine distance 操作符。

### 3.6 `MybatisDataSourceConfiguration.java`

新增主 MySQL MyBatis 配置。

作用：明确主业务 Mapper 仍然扫描：

```text
com.chushi.aiinterview.mappers
classpath:mappers/*.xml
```

这套继续连接 MySQL。

### 3.7 `RagDataSourceConfiguration.java`

新增 RAG PostgreSQL 配置。

作用：RAG Mapper 单独扫描：

```text
com.chushi.aiinterview.rag.mappers
classpath:mappers/rag/*.xml
```

这套连接 PostgreSQL + pgvector。

并且新增 RAG Flyway：

```java
Flyway.configure()
        .dataSource(ragDataSource)
        .locations("classpath:rag-migrations")
        .baselineOnMigrate(true)
        .load();
```

作用：RAG 的 PostgreSQL migration 和主业务 MySQL migration 分开执行。

### 3.8 `pom.xml`

新增：

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

作用：让 Flyway 支持 PostgreSQL 数据库类型。

### 4. 验证过程

#### 4.1 编译验证

执行：

```bash
./mvnw -q -DskipTests compile
```

结果：通过。

#### 4.2 PostgreSQL 建表验证

执行 RAG migration 脚本后，确认 `ai_rag_chunk` 表存在。

表结构确认：

```text
embedding vector(1536)
metadata jsonb
idx_ai_rag_chunk_embedding ivfflat
idx_ai_rag_chunk_question_id btree
```

#### 4.3 pgvector 查询验证

为了验证 pgvector 能保存和检索向量，手工构造了一个测试向量：

```sql
array_fill(0.001::real, ARRAY[1536])
```

插入测试 chunk 后，用同一个向量查询，返回距离：

```text
distance = 0
```

说明 pgvector 相似度查询可用。

注意：这不是 embedding 模型生成的向量，只是数据库能力验证。

测试完成后，已删除该测试 chunk，避免污染本地数据。

#### 4.4 Spring 上下文测试

执行：

```bash
./mvnw -q -Dtest=AiInterviewApplicationTests test
```

结果：通过。

验证到：

- 主 MySQL Mapper 能扫描。
- RAG PostgreSQL Mapper 能扫描。
- RAG Flyway 能启动。
- `AiRagChunkMapper.xml` 能被解析。
- 两套数据源可以共存。

### 5. 关于 Flyway baseline 的说明

这次本地验证顺序是：

```text
先手动执行 RAG 建表 SQL
再启动 Spring 上下文
```

所以当前本地 PostgreSQL 的 `flyway_schema_history` 里显示：

```text
version = 1
description = << Flyway Baseline >>
```

这是因为表已经手动存在，Flyway 对现有 schema 做了 baseline。

如果重新删除 Docker volume，使用空 PostgreSQL 启动应用，Flyway 会正常执行：

```text
rag-migrations/V0.0.1__Create_ai_rag_chunk_table.sql
```

### 6. 下一步

下一步进入 V4.3：Embedding 模型封装。

要做：

1. 新增 `AiEmbeddingModelProvider`。
2. 读取 `ai.embedding-model` 配置。
3. 调用 embedding 模型把文本转成向量。
4. 把模型返回的向量转成 pgvector 可写入的字符串格式。
5. 写单元测试验证向量格式转换。

## Step 30：V4.3 新增 Embedding 模型封装

日期：2026-06-16。

目标：把 embedding 模型调用封装成后端组件，为后续“题目文本 -> 向量 -> 写入 pgvector”做准备。

### 1. 这一步做什么

新增 `AiEmbeddingModelProvider`，负责：

1. 读取 `ai.embedding-model` 配置。
2. 创建 LangChain4j 的 `OpenAiEmbeddingModel`。
3. 调用 embedding 模型把文本转成 `float[]`。
4. 校验实际返回维度是否等于配置维度。
5. 把 `float[]` 转成 pgvector 可写入的字符串格式。

这一步仍然不做：

- 题目索引接口。
- 自动切分 chunk。
- 写入 `ai_rag_chunk`。
- 聊天时 RAG 检索。

这些会放到 V4.4 之后。

### 2. 为什么要先封装 Provider

RAG 后续会在两个地方用 embedding：

```text
索引阶段：题目 chunk -> embedding -> 写入 pgvector
检索阶段：用户问题 -> embedding -> pgvector search topK
```

如果不先封装 provider，索引逻辑和检索逻辑都会直接依赖模型 SDK，后面不好替换模型，也不好做维度校验。

所以先做统一入口：

```java
AiEmbeddingModelProvider
```

### 3. 改动位置

#### 3.1 `AiEmbeddingModelProvider.java`

新增文件：

```text
src/main/java/com/chushi/aiinterview/components/AiEmbeddingModelProvider.java
```

核心方法：

```java
public EmbeddingModel getEmbeddingModel()
```

作用：懒加载 LangChain4j 的 `OpenAiEmbeddingModel`。

```java
public float[] embed(String text)
```

作用：调用 embedding 模型，把文本转成向量。

```java
public String embedAsPgVector(String text)
```

作用：调用 embedding 后，直接返回 pgvector 可写入的字符串。

```java
public static String toPgVectorLiteral(float[] vector)
```

作用：把 Java float 数组转成 pgvector 字符串，例如：

```text
[0.1,-2.5,3.0]
```

后续 Mapper 里会用：

```sql
#{embedding}::vector
```

把这个字符串转成 PostgreSQL 的 vector 类型。

### 4. 维度校验

Provider 中新增了维度校验：

```java
assertConfiguredDimension(vector)
```

逻辑是：

```text
配置维度为空或 <= 0 -> 报错
实际返回 vector.length != 配置维度 -> 报错
```

这样可以避免出现：

```text
配置 vector(1536)
模型实际返回 1024 / 3072
```

然后写入 pgvector 时报数据库错误。

现在会在模型调用后立刻暴露问题。

### 5. 为什么还是用 OpenAiEmbeddingModel

当前项目聊天模型也是通过 OpenAI-compatible 方式接入：

```java
OpenAiChatModel
```

embedding 也沿用同一思路：

```java
OpenAiEmbeddingModel
```

只要供应商提供 OpenAI-compatible embedding 接口，就可以通过配置切换：

```yaml
ai:
  embedding-model:
    api-key: ${AI_EMBEDDING_API_KEY:${AI_API_KEY:${OPENAI_API_KEY:}}}
    base-url: ${AI_EMBEDDING_BASE_URL:https://api.openai.com/v1}
    model-name: ${AI_EMBEDDING_MODEL:text-embedding-3-small}
    dimension: ${AI_EMBEDDING_DIMENSION:1536}
```

### 6. 新增测试

新增文件：

```text
src/test/java/com/chushi/aiinterview/components/AiEmbeddingModelProviderTest.java
```

测试内容：

1. `float[]` 能转成 pgvector 字符串。
2. 空向量会报错。
3. `NaN` 这种非法值会报错。

执行：

```bash
./mvnw -q -Dtest=AiEmbeddingModelProviderTest test
```

结果：通过。

### 7. 本次验证

已执行：

```bash
./mvnw -q -Dtest=AiEmbeddingModelProviderTest test
```

结果：通过。

已执行：

```bash
./mvnw -q -DskipTests compile
```

结果：通过。

已执行：

```bash
./mvnw -q -Dtest=AiInterviewApplicationTests test
```

结果：通过。

说明：

- 新增 provider 不影响 Spring 上下文启动。
- provider 是懒加载，只有真正调用 embedding 时才要求 key 可用。
- 主 MySQL 数据源、RAG PostgreSQL 数据源仍然可以共存。

### 8. 真实调用状态

当前 shell 环境检查结果：

```text
AI_EMBEDDING_API_KEY=empty
AI_EMBEDDING_BASE_URL=empty
AI_EMBEDDING_MODEL=empty
AI_EMBEDDING_DIMENSION=empty
```

所以这一步没有执行真实 embedding 远程调用。

原因：如果直接调用，会走默认 OpenAI baseUrl，并且 key 为空。

后续要真实测维度，需要保证运行环境能读到：

```text
AI_EMBEDDING_API_KEY
AI_EMBEDDING_BASE_URL
AI_EMBEDDING_MODEL
AI_EMBEDDING_DIMENSION
```

或者把这些配置写到本地 `application-dev.yaml`，但不要提交真实 key。

### 9. 下一步

下一步进入 V4.4：题目手动索引接口。

要做：

1. 根据 `questionId` 查询题目。
2. 把题目标题、内容、答案、标签、难度组装成 RAG 文本。
3. 第一版先生成一个 chunk。
4. 调用 `AiEmbeddingModelProvider.embedAsPgVector`。
5. 删除旧的 `questionId` chunk。
6. 写入新的 `ai_rag_chunk`。
7. 增加一个手动索引接口，方便调试。

## Step 31：V4.3 增加 Embedding 调试接口

日期：2026-06-16。

目标：在正式做题目索引前，先提供一个最小调试接口，用来确认 embedding 模型是否能调用、实际返回维度是多少。

### 1. 这一步做什么

新增接口：

```text
POST /api/ai/embedding/debug
```

接口只允许：

```text
ADMIN
SUPER_ADMIN
```

原因是这个接口会真实调用 embedding 模型，会产生 token 成本，不应该开放给普通用户随意调用。

### 2. 新增文件

#### 2.1 `AiEmbeddingDebugDto.java`

请求体：

```json
{
  "text": "HashMap 的底层原理是什么？"
}
```

字段校验：

```java
@NotBlank
@Size(max = 2000)
```

作用：限制调试文本不能为空，也不能太长。

#### 2.2 `AiEmbeddingDebugVo.java`

响应字段：

```java
private String modelName;
private Integer configuredDimension;
private Integer actualDimension;
private Boolean dimensionMatched;
private List<Float> vectorPreview;
private String pgVectorPreview;
```

作用：

- `modelName`：当前配置使用的 embedding 模型。
- `configuredDimension`：配置里的维度。
- `actualDimension`：模型真实返回维度。
- `dimensionMatched`：两者是否一致。
- `vectorPreview`：前 8 个向量值，方便确认不是空结果。
- `pgVectorPreview`：pgvector 字符串预览。

### 3. 修改位置

修改 `AiChatController.java`。

新增：

```java
@PostMapping("/api/ai/embedding/debug")
@Operation(summary = "调试 AI Embedding 模型")
@RequireRole(value = {UserRole.ADMIN, UserRole.SUPER_ADMIN}, predicate = RequireRole.Predicate.OR)
public Response<AiEmbeddingDebugVo> debugEmbedding(...)
```

调用流程：

```text
接收 text
  ↓
AiEmbeddingModelProvider.embed(text)
  ↓
拿到 float[] vector
  ↓
返回实际维度、配置维度、维度是否匹配、向量预览
```

### 4. 这一步不做什么

这个接口不是正式业务接口。

它不做：

- 保存向量到 `ai_rag_chunk`。
- 根据 `questionId` 查题目。
- chunk 切分。
- RAG 检索。
- 聊天 Prompt 接入。

它只是为了先回答一个问题：

```text
我配置的 embedding 模型到底能不能调用，返回多少维？
```

### 5. 怎么验证

已执行：

```bash
./mvnw -q -DskipTests compile
```

结果：通过。

已执行：

```bash
./mvnw -q -Dtest=AiEmbeddingModelProviderTest test
```

结果：通过。

后续本地启动后，可以用管理员 token 调：

```bash
curl -X POST http://localhost:8080/api/ai/embedding/debug   -H 'Content-Type: application/json'   -H 'Authorization: Bearer <ADMIN_TOKEN>'   -d '{"text":"HashMap 的底层原理是什么？"}'
```

如果配置正确，会返回：

```json
{
  "model_name": "text-embedding-v4",
  "configured_dimension": 1536,
  "actual_dimension": 1536,
  "dimension_matched": true,
  "vector_preview": [0.01, -0.02],
  "pg_vector_preview": "[0.01,-0.02,..."
}
```

### 6. 下一步

下一步仍然是 V4.4：题目手动索引接口。

在做 V4.4 前，建议先用这个 debug 接口确认 embedding 模型真实维度，再决定 `ai_rag_chunk.embedding vector(1536)` 是否需要改。

## Step 32：V4.4 增加题目文本 RAG 手动索引接口

日期：2026-06-16。

目标：先跑通“题目文本 -> embedding -> pgvector”的索引链路。

### 1. 这一步做什么

新增手动索引接口：

```text
POST /api/ai/rag/questions/{questionId}/index
```

权限：

```text
SUPER_ADMIN
```

当前第一版只做文本 RAG：

```text
题目标题
题目内容
参考答案
标签
难度
```

这些字段会被拼成一个 chunk，然后调用 `text-embedding-v4` 生成 1536 维向量，写入 PostgreSQL pgvector 的 `ai_rag_chunk` 表。

### 2. 新增文件

#### 2.1 `AiRagQuestionIndexVo.java`

返回索引结果：

```java
private Long questionId;
private Integer deletedChunkCount;
private Integer indexedChunkCount;
private Integer chunkContentLength;
private String embeddingModelName;
private Integer embeddingDimension;
private Boolean dimensionMatched;
```

作用：调试时能直接看到本次索引了多少 chunk、是否使用正确模型和维度。

#### 2.2 `AiRagIndexService.java`

新增接口：

```java
AiRagQuestionIndexVo rebuildQuestionIndex(Long questionId);
```

作用：封装 RAG 索引业务，不把逻辑写进 Controller。

#### 2.3 `AiRagIndexServiceImpl.java`

核心流程：

```text
根据 questionId 查询题目
  ↓
拼接题目文本 chunk
  ↓
调用 AiEmbeddingModelProvider.embedAsPgVector
  ↓
删除旧 chunk
  ↓
插入新 chunk
  ↓
返回索引结果
```

关键注释已写在调用处：

```java
// V4.4 第一版只做文本 RAG：把题目核心字段拼成一个 chunk，后续再扩展长文本切分。
```

```java
// 这里会真实调用 embedding 模型，得到和 pgvector vector(1536) 匹配的向量字面量。
```

```java
// 重建索引要先删旧 chunk，再写新 chunk，避免同一道题重复召回多个旧版本。
```

注意：当前没有使用默认 `@Transactional`。

原因：这个方法同时访问 MySQL 和 PostgreSQL。项目目前没有配置跨数据源事务，直接加默认事务只会绑定主数据源，容易造成误解。

#### 2.4 `AiRagController.java`

新增控制器：

```java
@PostMapping("/api/ai/rag/questions/{questionId}/index")
```

作用：提供手动触发索引的入口。

### 3. 当前 chunk 结构

第一版每道题只生成一个 chunk：

```text
# 题目标题
...

# 题目内容
...

# 参考答案
...

# 标签
...

# 难度
...
```

后续如果内容过长，再扩展成多 chunk。

### 4. 为什么先做手动索引

自动索引会牵涉：

- 题目创建后自动索引。
- 题目更新后重建索引。
- 失败重试。
- 批量全量索引。
- 队列异步处理。

这些会增加复杂度。

学习阶段先做手动接口，可以更清楚地验证：

```text
文本拼接是否正确
embedding 调用是否成功
pgvector 是否能保存
维度是否匹配
```

### 5. 真实验证

先确认本地 `text-embedding-v4` 真实可用：

```text
embedding_model=text-embedding-v4
configured_dimension=1536
actual_dimension=1536
dimension_matched=true
```

然后对本地题目执行真实索引：

```text
question_id=82856969085390848
deleted_chunk_count=0
indexed_chunk_count=1
chunk_content_length=2898
embedding_model=text-embedding-v4
embedding_dimension=1536
dimension_matched=true
```

PostgreSQL 查询确认：

```text
question_id=82856969085390848
chunk_index=0
source_type=question
content_length=2898
dims=1536
metadata={"difficulty": 3}
```

说明：

- 题目文本已经成功进入 RAG chunk。
- embedding 字段确实是 1536 维。
- pgvector 存储成功。

### 6. 已执行测试

执行：

```bash
./mvnw -q -DskipTests compile
```

结果：通过。

执行：

```bash
./mvnw -q -Dtest=AiEmbeddingModelProviderTest test
```

结果：通过。

真实索引测试使用临时测试类执行，执行后已删除，没有保留到仓库。

### 7. 下一步

下一步进入 V4.5：检索调试接口。

要做：

1. 接收用户 query 文本。
2. 调用 `AiEmbeddingModelProvider.embedAsPgVector` 生成 query embedding。
3. 调用 `AiRagChunkMapper.searchTopK`。
4. 返回命中的 chunk、distance、questionId、content 预览。
5. 验证输入“JVM GC 调优”能召回刚刚索引的题目。

### 5.1 真实索引验证补充

本次已经对本地题目执行过一次真实索引。

验证题目：

```text
questionId = 82856969085390848
title = JVM 调优和 GC 日志应该怎么看
```

索引服务返回：

```text
deleted_chunk_count = 1
indexed_chunk_count = 1
chunk_content_length = 2898
embedding_model = text-embedding-v4
embedding_dimension = 1536
dimension_matched = true
```

pgvector 查询确认：

```text
question_id = 82856969085390848
chunk_index = 0
source_type = question
content_length = 2898
dims = 1536
```

说明：

```text
题目文本 -> embedding -> ai_rag_chunk 写入
```

这条链路已经跑通。

注意：验证时发现当前 MyBatis DEBUG 日志会打印完整 embedding 向量，日志非常长。后续可以把 RAG Mapper 日志降级，避免索引时刷屏。

## Step 33：V4.5 增加 RAG 检索调试接口

日期：2026-06-16。

目标：在接入聊天 Prompt 前，先跑通“用户问题 -> embedding -> pgvector topK 检索”的链路。

### 1. 这一步做什么

新增调试接口：

```text
POST /api/ai/rag/search/debug
```

权限：

```text
ADMIN
SUPER_ADMIN
```

请求体：

```json
{
  "query": "JVM GC 日志怎么看",
  "topK": 3
}
```

返回：

```text
query
topK
embeddingModelName
embeddingDimension
matchedChunkCount
chunks
```

### 2. 为什么先做检索调试接口

RAG 接入聊天之前，需要先确认检索是否可用。

完整 RAG 回答链路是：

```text
用户问题
  ↓
query embedding
  ↓
pgvector topK
  ↓
把检索结果放进 Prompt
  ↓
大模型回答
```

如果不先做检索调试，后面回答效果不好时，很难判断问题出在：

```text
索引数据不对
embedding 模型不对
pgvector 检索不对
Prompt 没组织好
大模型没使用资料
```

所以这一步只验证检索，不接聊天。

### 3. 新增文件

#### 3.1 `AiRagSearchDebugDto.java`

新增请求 DTO。

字段：

```java
private String query;
private Integer topK = 5;
```

校验：

```java
@NotBlank
@Size(max = 1000)
@Min(1)
@Max(10)
```

作用：限制调试 query 长度和 topK 范围，避免一次检索返回过多 chunk。

#### 3.2 `AiRagSearchDebugVo.java`

新增响应 VO。

字段：

```java
private String query;
private Integer topK;
private String embeddingModelName;
private Integer embeddingDimension;
private Integer matchedChunkCount;
private List<AiRagChunkSearchVo> chunks;
```

作用：调试时可以看到检索用了哪个模型、返回了多少条 chunk、每条 chunk 的 distance。

### 4. 修改位置

#### 4.1 `AiRagIndexService.java`

新增方法：

```java
AiRagSearchDebugVo searchDebug(String query, Integer topK);
```

#### 4.2 `AiRagIndexServiceImpl.java`

新增检索逻辑：

```text
校验 query
  ↓
调用 AiEmbeddingModelProvider.embedAsPgVector(query)
  ↓
调用 AiRagChunkMapper.searchTopK
  ↓
返回检索调试信息
```

核心注释：

```java
// 检索阶段和索引阶段必须使用同一个 embedding 模型，否则向量空间不一致，召回结果会失真。
```

#### 4.3 `AiRagController.java`

新增接口：

```java
@PostMapping("/api/ai/rag/search/debug")
```

### 5. 日志调整

索引和检索时，MyBatis DEBUG 日志会打印完整 1536 维 embedding，日志非常长。

所以在配置中新增：

```yaml
logging:
  level:
    com.chushi.aiinterview.rag.mappers: INFO
```

作用：避免 RAG Mapper 打印完整向量。

### 6. 本次验证

#### 6.1 编译验证

执行：

```bash
./mvnw -q -DskipTests compile
```

结果：通过。

#### 6.2 真实检索验证

前置条件：已经索引题目：

```text
questionId = 82856969085390848
title = JVM 调优和 GC 日志应该怎么看
```

执行检索 query：

```text
JVM GC 日志怎么看
```

结果：

```text
query = JVM GC 日志怎么看
top_k = 3
embedding_model = text-embedding-v4
embedding_dimension = 1536
matched_chunk_count = 1
chunk question_id = 82856969085390848
title = JVM 调优和 GC 日志应该怎么看
distance = 0.24312147416132246
```

说明：

```text
用户问题 -> query embedding -> pgvector topK -> 召回题目 chunk
```

这条检索链路已经跑通。

### 7. 下一步

下一步进入 V4.6：聊天 Prompt 接入 RAG。

要做：

1. 在 `AiChatServiceImpl.sendMessage` 中调用 RAG 检索。
2. 把 topK chunk 组装成“RAG 检索资料”。
3. 注入现有聊天 Prompt。
4. 返回调试字段，确认本次回答用了哪些 chunk。



## Step 34 - V4.6 聊天 Prompt 接入 RAG 检索资料

### 1. 这一步做什么

把前面 V4.5 已经验证通过的 RAG 检索链路，接入真正的聊天接口。

也就是用户发送消息时，后端现在会多做一步：

```text
当前用户问题
  ↓
调用 embedding 模型生成 query vector
  ↓
去 pgvector 的 ai_rag_chunk 表检索 topK 资料
  ↓
把召回 chunk 拼进聊天 Prompt
  ↓
调用 chat model 生成最终回答
```

### 2. 为什么这样做

V4.5 只是证明“用户问题可以召回题库资料”，但聊天回答还没有使用这些资料。

V4.6 的目标是让 AI 助教回答时同时看到：

```text
当前题目上下文
用户长期学习记忆
当前会话摘要
最近对话历史
RAG 检索资料
当前用户问题
```

这样做之后，模型不是只依赖当前题目和历史对话，而是可以额外参考向量库召回的相关题目资料。

### 3. 改了哪里

#### 3.1 `AiChatServiceImpl.sendMessage`

位置：`src/main/java/com/chushi/aiinterview/services/impl/AiChatServiceImpl.java`

新增流程：

```java
var ragResult = searchRagContext(request.getContent());
var ragChunks = ragResult == null ? List.<AiRagChunkSearchVo>of() : ragResult.getChunks();
var prompt = buildPrompt(question, userMemorySummary, session.getMemorySummary(), historyMessages, ragChunks, request.getContent());
```

作用：

```text
用户当前问题先做 RAG 检索
检索结果 ragChunks 再进入 buildPrompt
```

#### 3.2 `searchRagContext`

新增方法：

```java
private AiRagSearchDebugVo searchRagContext(String currentUserMessage) {
    try {
        return aiRagIndexService.searchDebug(currentUserMessage, RAG_SEARCH_TOP_K);
    } catch (Exception e) {
        log.warn("AiChatRagSearchException: {}", e.getMessage(), e);
        return null;
    }
}
```

为什么这里 catch 异常：

```text
RAG 是增强能力，不是聊天主链路的必需条件。
如果 pgvector、embedding 模型、RAG 数据源临时失败，聊天应该降级成原来的普通回答，而不是整轮对话失败。
```

#### 3.3 `buildPrompt`

方法参数新增：

```java
List<AiRagChunkSearchVo> ragChunks
```

Prompt 中新增区块：

```text
# RAG 检索资料
下面是根据当前用户问题从题库向量库召回的资料。优先使用这些资料补充回答；如果资料与问题无关，请忽略。
```

注意这里没有强制模型必须使用 RAG，因为向量检索可能召回不完全相关的资料，所以 Prompt 明确允许“无关就忽略”。

#### 3.4 `buildRagContextText`

新增方法：

```java
private String buildRagContextText(List<AiRagChunkSearchVo> ragChunks)
```

作用：把检索出的 chunk 转成适合放进 Prompt 的文本。

目前每个 chunk 会包含：

```text
题目标题
难度
标签
向量距离 distance
chunk 内容
```

同时做了长度限制：

```java
MAX_RAG_CHUNK_CONTENT_LENGTH = 1200
MAX_RAG_CONTEXT_TEXT_LENGTH = 4000
```

为什么限制长度：

```text
RAG 召回资料也是 token。
如果不裁剪，topK 多个 chunk 可能挤占聊天历史、题目内容、用户记忆的上下文空间。
```

#### 3.5 `AiChatMessageSendVo`

位置：`src/main/java/com/chushi/aiinterview/commons/vo/AiChatMessageSendVo.java`

新增字段：

```java
private Boolean ragEnabled;
private Integer ragChunkCount;
private List<AiRagChunkSearchVo> ragChunks;
```

作用：接口返回时可以看到本轮聊天是否执行了 RAG 检索，以及召回了哪些 chunk，方便调试和复盘。

### 4. 当前实现限制

目前是第一版接入，策略比较直接：

```text
每次发送消息都会尝试 RAG 检索 topK=3
```

这意味着：

```text
一次聊天回答 = 1 次 embedding 调用 + 1 次 chat model 调用
```

如果后续要做成本优化，可以继续加：

```text
规则判断是否需要 RAG
只对知识类问题启用 RAG
按题库/标签/难度过滤检索范围
低分 chunk 不进入 Prompt
```

### 5. 本次验证

#### 5.1 编译验证

执行：

```bash
./mvnw -q -DskipTests compile
```

结果：通过。

#### 5.2 聊天服务单元测试

执行：

```bash
./mvnw -q -Dtest=AiChatServiceImplTest test
```

结果：通过。

#### 5.3 Spring 应用启动测试

执行：

```bash
./mvnw -q -Dtest=AiInterviewApplicationTests test
```

结果：通过。

说明：

```text
RAG 数据源、RAG Mapper、Embedding Provider、AiChatServiceImpl 的依赖注入都能正常加载。
```

### 6. 下一步

下一步可以做 V4.7：RAG 检索策略优化。

建议优先做：

```text
不要每次聊天都无脑 RAG
先加一个 needRag 判断
只有用户问题像“解释知识点 / 对比 / 追问原理 / 怎么做 / 为什么”时，才执行 RAG
```

这样可以降低 embedding 调用次数，也更接近真实项目里的成本控制设计。


## Step 35 - V4.7 题目索引改成多段 chunk

### 1. 这一步做什么

把题目 RAG 索引从：

```text
一个题目 -> 一个 chunk -> 一个 embedding
```

改成：

```text
一个题目 -> 多个 chunk -> 多个 embedding
```

### 2. 为什么这样做

一个题目如果答案很长，整段文本只生成一个向量会导致检索粒度太粗。

用户问某个细节时，模型可能召回整题，但无法精确命中具体解释段落。

多段 chunk 之后，检索粒度变成：

```text
题目内容 chunk
参考答案 chunk 1
参考答案 chunk 2
标签 chunk
难度 chunk
```

这样用户问细节时，pgvector 更容易召回真正相关的片段。

### 3. 改了哪里

#### 3.1 `AiRagIndexServiceImpl.rebuildQuestionIndex`

位置：`src/main/java/com/chushi/aiinterview/services/impl/AiRagIndexServiceImpl.java`

原逻辑：

```text
buildQuestionChunkText(question)
  ↓
生成一个 embedding
  ↓
插入一个 ai_rag_chunk
```

新逻辑：

```text
buildQuestionChunks(question)
  ↓
循环每个 chunk
  ↓
每个 chunk 单独调用 embedding
  ↓
每个 chunk 单独写入 ai_rag_chunk
```

### 4. chunk 切分规则

当前第一版规则：

```text
题目标题作为公共上下文
题目内容单独成 chunk
参考答案按长度切成多个 chunk
标签单独成 chunk
难度单独成 chunk
```

每个 chunk 都会带上题目标题。

原因：单个 chunk 被召回后，如果没有标题，模型可能不知道这段资料属于哪道题。

### 5. 长文本切片参数

当前常量：

```java
QUESTION_CHUNK_MAX_LENGTH = 1200
QUESTION_CHUNK_OVERLAP_LENGTH = 150
```

为什么有 overlap：

```text
如果一个关键解释刚好落在两个 chunk 的边界，纯硬切会割裂语义。
overlap 可以让相邻 chunk 重叠一小段，降低边界信息丢失。
```

### 6. 新增测试

新增文件：

```text
src/test/java/com/chushi/aiinterview/services/impl/AiRagIndexServiceImplTest.java
```

覆盖：

```text
长答案会拆成多个 chunk
每个 chunk 都包含题目标题
切片之间保留 overlap
```

## Step 36 - V4.8 RAG 检索结果增加 distance 阈值过滤

### 1. 这一步做什么

在 RAG 检索阶段增加质量过滤。

原逻辑：

```text
pgvector topK 返回什么，就全部放进 Prompt
```

新逻辑：

```text
pgvector topK 返回 chunk
  ↓
只保留 distance <= 0.45 的 chunk
  ↓
再放进 Prompt
```

### 2. 为什么这样做

向量检索一定会返回 topK，但 topK 不代表一定相关。

如果库里只有少量数据，或者用户问题和题库不相关，pgvector 仍然会返回“相对最近”的 chunk。

这些低相关资料进入 Prompt 后，会干扰模型回答。

所以需要增加阈值：

```text
distance 越小越相似
超过阈值认为相关性不足
```

### 3. 改了哪里

位置：`AiRagIndexServiceImpl.searchDebug`

新增逻辑：

```java
.filter(chunk -> chunk.getDistance() != null && chunk.getDistance() <= MAX_SEARCH_DISTANCE)
```

当前阈值：

```java
MAX_SEARCH_DISTANCE = 0.45
```

### 4. 当前限制

这个阈值是经验值。

后续需要基于真实数据调参：

```text
如果召回太少 -> 放宽到 0.5 / 0.6
如果干扰太多 -> 收紧到 0.35 / 0.4
```

## Step 37 - V4.9 聊天增加 needRag 规则，降低 embedding 调用成本

### 1. 这一步做什么

聊天发送消息时，不再每次都无脑 RAG。

原逻辑：

```text
每次 sendMessage
  ↓
都调用 embedding
  ↓
都查 pgvector
```

新逻辑：

```text
sendMessage
  ↓
shouldSearchRag 判断
  ↓
需要 RAG：调用 embedding + pgvector
不需要 RAG：跳过 RAG，直接走普通聊天 Prompt
```

### 2. 为什么这样做

有些用户消息不需要检索知识库：

```text
好的
继续
明白了
再说一点
```

这些如果也调用 embedding，会产生额外成本和延迟。

### 3. 改了哪里

位置：`AiChatServiceImpl.sendMessage`

新增：

```java
var ragResult = shouldSearchRag(request.getContent()) ? searchRagContext(request.getContent()) : null;
```

新增方法：

```java
private boolean shouldSearchRag(String currentUserMessage)
```

### 4. 第一版规则

满足任一条件就启用 RAG：

```text
消息长度 >= 18
包含：为什么、怎么、如何、是什么、原理、底层、源码、区别、对比、流程、解释、举例、复杂度、场景、优化
```

短确认类消息不会启用 RAG。

### 5. 为什么先用规则，不用 AI 判断

AI 判断是否需要 RAG 本身也要调用模型。

现在第一版目标是降低成本，所以先用确定性规则：

```text
便宜
可解释
容易测试
不引入新模型调用
```

后续如果规则不够，可以再升级为：

```text
规则过滤 -> 小模型判断 -> RAG
```

### 6. 新增测试

修改文件：

```text
src/test/java/com/chushi/aiinterview/services/impl/AiChatServiceImplTest.java
```

新增用例：

```text
好的 / 继续 -> 不触发 RAG
HashMap 底层原理是什么 -> 触发 RAG
帮我对比 synchronized 和 ReentrantLock -> 触发 RAG
```

## Step 38 - 本轮验证结果

### 1. 局部测试

执行：

```bash
./mvnw -q -Dtest=AiRagIndexServiceImplTest test
```

结果：通过。

执行：

```bash
./mvnw -q -Dtest=AiChatServiceImplTest test
```

结果：通过。

注意：第一次并行跑两个 Maven 测试时，`AiChatServiceImplTest` 出现过 `ClassNotFoundException`。
原因是两个 Maven 进程同时写 `target/surefire-reports` 和测试输出目录，产生了扫描竞争。
改成顺序执行后通过。

### 2. 当前完成链路

现在 RAG 链路已经变成：

```text
题目 -> 多段 chunk -> 多个 embedding -> pgvector
用户问题 -> needRag 判断 -> query embedding -> pgvector topK -> distance 过滤 -> Prompt
```

### 3. 是否还有遗漏

本轮还有两个暂时没做的点：

```text
1. 自动批量索引所有题目
2. 根据题库 / 标签 / 难度过滤检索范围
```

原因：当前优先目标是把单题索引和聊天召回链路做清楚。
批量索引和过滤范围属于下一阶段工程化能力。


## Step 39 - V4.10 RAG 工程化配置和决策服务

### 1. 这一步做什么

把上一轮写死在代码里的 RAG 参数和是否检索判断，拆成工程化结构。

上一版问题：

```text
chunk 长度写死在 AiRagIndexServiceImpl
chunk overlap 写死在 AiRagIndexServiceImpl
distance 阈值写死在 AiRagIndexServiceImpl
shouldSearchRag 写在 AiChatServiceImpl 私有方法里
```

这一版改成：

```text
RagProperties 统一承载配置
AiRagDecisionService 专门判断是否需要 RAG
AiChatServiceImpl 只负责编排聊天流程
AiRagIndexServiceImpl 只负责索引和检索
```

### 2. 为什么这样做

“是否 embedding”不是聊天服务本身的职责。

如果一直放在 `AiChatServiceImpl` 里，后面会出现：

```text
规则越来越多
调参要改 Java 代码
无法解释为什么这次触发 RAG
不好单独测试
后续接小模型判断会污染聊天主流程
```

拆成 `AiRagDecisionService` 后，聊天主流程变成：

```text
用户消息
  ↓
AiRagDecisionService.decide
  ↓
返回 enabled / reason / strategy
  ↓
需要 RAG 才调用 embedding + pgvector
```

### 3. 配置改动

#### 3.1 `RagProperties`

新增配置结构：

```java
private ChunkProperties chunk;
private SearchProperties search;
private DecisionProperties decision;
```

#### 3.2 `application.yaml` / `application-dev.yaml`

新增：

```yaml
rag:
  chunk:
    max-length: 1200
    overlap-length: 150
  search:
    max-distance: 0.45
  decision:
    min-message-length: 18
    keywords:
      - 为什么
      - 怎么
      - 如何
      - 是什么
      - 原理
      - 底层
      - 源码
      - 区别
      - 对比
      - 流程
      - 解释
      - 举例
      - 复杂度
      - 场景
      - 优化
```

作用：以后调参不用改业务代码。

### 4. 决策服务

新增：

```text
src/main/java/com/chushi/aiinterview/services/AiRagDecisionService.java
src/main/java/com/chushi/aiinterview/services/impl/AiRagDecisionServiceImpl.java
src/main/java/com/chushi/aiinterview/commons/vo/AiRagDecisionVo.java
```

返回对象：

```java
private Boolean enabled;
private String reason;
private String strategy;
```

示例：

```json
{
  "enabled": true,
  "reason": "matched_keyword:是什么",
  "strategy": "rule_v1"
}
```

### 5. 当前决策规则

当前策略名：

```text
rule_v1
```

规则顺序：

```text
1. 空消息 -> 不启用 RAG
2. 消息长度 >= min-message-length -> 启用 RAG
3. 命中 keywords -> 启用 RAG
4. 否则不启用 RAG
```

为什么先长度再关键词：

```text
长问题通常已经是明确知识诉求，可能没有固定关键词。
短消息更容易是“好的 / 继续”，所以短消息必须命中关键词才 RAG。
```

### 6. 聊天返回改动

`AiChatMessageSendVo` 新增：

```java
private String ragDecisionReason;
private String ragDecisionStrategy;
```

这样前端或调试时可以看到：

```text
本轮为什么触发 RAG
本轮为什么没有触发 RAG
当前使用的是哪套决策策略
```

### 7. 代码调用变化

`AiChatServiceImpl.sendMessage` 原来：

```java
var ragResult = shouldSearchRag(request.getContent()) ? searchRagContext(request.getContent()) : null;
```

现在：

```java
var ragDecision = aiRagDecisionService.decide(request.getContent());
var ragResult = Boolean.TRUE.equals(ragDecision.getEnabled()) ? searchRagContext(request.getContent()) : null;
```

### 8. 索引和检索参数变化

`AiRagIndexServiceImpl` 原来使用常量：

```java
QUESTION_CHUNK_MAX_LENGTH
QUESTION_CHUNK_OVERLAP_LENGTH
MAX_SEARCH_DISTANCE
```

现在从配置读取：

```text
rag.chunk.max-length
rag.chunk.overlap-length
rag.search.max-distance
```

同时保留兜底默认值，避免配置为空导致服务异常。

### 9. 新增测试

新增：

```text
src/test/java/com/chushi/aiinterview/services/impl/AiRagDecisionServiceImplTest.java
```

覆盖：

```text
短确认消息不启用 RAG
关键词消息启用 RAG
修改配置后规则会跟着变化
```

调整：

```text
AiRagIndexServiceImplTest 注入 RagProperties
AiChatServiceImplTest 移除原先针对私有 shouldSearchRag 的测试
```

### 10. 当前是否还有遗漏

还有：

```text
1. 决策规则还没有接入小模型判断
2. RAG 参数还没有管理后台动态调整
3. distance 阈值还没有基于真实数据集评估
4. 没有记录 RAG 命中率、跳过率、平均 distance 等指标
```

但当前已经从“写死 if”升级成了：

```text
配置化参数 + 独立决策服务 + 可解释返回 + 单元测试
```

这是后续继续工程化的基础。


## Step 40 - V4.11 RAG 批量索引接口

### 1. 这一步做什么

新增 RAG 批量索引能力。

之前只有单题接口：

```text
POST /api/ai/rag/questions/{questionId}/index
```

现在新增：

```text
POST /api/ai/rag/questions/index/batch
```

### 2. 为什么要做批量索引

RAG 不是只索引一题就有价值。

真正使用时，需要把一批题目写进 pgvector，检索才有数据规模。

如果只能手动索引单题，会出现：

```text
每次只能索引一个 questionId
无法快速给本地向量库灌数据
某道题失败时不好批量统计
```

所以先做一个同步批量接口，方便学习和调试。

### 3. 为什么第一版做同步接口，不直接做 MQ / 异步任务

异步任务会引入更多工程组件：

```text
任务表
任务状态
进度查询
失败重试
队列消费
幂等控制
```

这些是后续要做的，但第一版先保持链路清晰：

```text
请求进来
  ↓
解析 questionIds 或 limit
  ↓
逐个调用单题索引
  ↓
返回每道题成功 / 失败
```

### 4. 请求 DTO

新增：

```text
src/main/java/com/chushi/aiinterview/commons/dto/AiRagQuestionBatchIndexDto.java
```

字段：

```java
private List<Long> questionIds;
private Integer limit = 10;
```

规则：

```text
如果传 questionIds -> 按指定题目索引
如果 questionIds 为空 -> 查询最新 limit 道题索引
```

限制：

```text
questionIds 最大 50 个
limit 范围 1-50
```

原因：批量索引会真实调用 embedding 模型，必须限制一次请求的成本和耗时。

### 5. 响应 VO

新增：

```text
AiRagQuestionBatchIndexVo
AiRagQuestionBatchIndexItemVo
```

返回结构：

```text
requestedCount
successCount
failedCount
items
```

每个 item 包含：

```text
questionId
success
message
result
```

### 6. 服务层实现

`AiRagIndexService` 新增：

```java
AiRagQuestionBatchIndexVo rebuildQuestionIndexBatch(List<Long> questionIds, Integer limit);
```

`AiRagIndexServiceImpl` 实现：

```text
resolveBatchQuestionIds
  ↓
逐个 rebuildSingleQuestionIndex
  ↓
成功写 success item
  ↓
失败写 failed item，不中断整批
```

核心设计：

```java
// 批量索引复用单题索引逻辑，避免 chunk 切分、embedding、写库规则出现两套实现。
```

也就是说，多段 chunk、embedding、删除旧 chunk、写新 chunk 都还是走单题索引逻辑。

### 7. Controller

`AiRagController` 新增接口：

```java
@PostMapping("/api/ai/rag/questions/index/batch")
```

权限：

```text
SUPER_ADMIN
```

原因：批量索引会产生多次 embedding 调用，有成本，不能开放给普通用户或普通管理员误触发。

### 8. 本次测试

修改：

```text
src/test/java/com/chushi/aiinterview/services/impl/AiRagIndexServiceImplTest.java
```

新增覆盖：

```text
指定 questionIds 时会去重、过滤 null
某道题失败不会中断整批
不传 questionIds 时会按 limit 查询最新题目
```

测试中没有调用真实 embedding。

原因：批量测试关注的是编排逻辑，不测试远程模型调用。

### 9. 当前还没做什么

这一步还不是最终批量索引系统。

暂时没做：

```text
异步任务
任务进度查询
失败重试
定时增量索引
按 update_time 只索引变更题目
索引版本号
并发限流
```

这些是下一阶段工程化内容。

当前完成的是：

```text
同步批量索引 + 每题结果可见 + 单题逻辑复用 + 成本上限控制
```


## Step 41 - V4.12 RAG 最近题目定时索引任务

### 1. 这一步做什么

新增一个 RAG 定时任务，用于定期索引最近的题目。

任务类：

```text
src/main/java/com/chushi/aiinterview/schedulers/AiRagIndexScheduler.java
```

启动类新增：

```java
@EnableScheduling
```

### 2. 为什么要做定时任务

前面已经有批量索引接口，但还需要人工调用。

定时任务解决的是：

```text
系统定期补索引最近题目
不用每次手动触发批量接口
让 pgvector 中的数据逐步保持更新
```

### 3. 为什么默认关闭

定时索引会真实调用 embedding 模型。

如果默认开启，应用一启动就可能产生：

```text
模型调用成本
较长后台任务
本地开发时误触发
```

所以第一版默认：

```yaml
rag:
  index-schedule:
    enabled: false
```

需要跑的时候再打开。

### 4. 配置项

新增：

```yaml
rag:
  index-schedule:
    enabled: ${RAG_INDEX_SCHEDULE_ENABLED:false}
    cron: ${RAG_INDEX_SCHEDULE_CRON:0 0 3 * * *}
    zone: ${RAG_INDEX_SCHEDULE_ZONE:Asia/Shanghai}
    limit: ${RAG_INDEX_SCHEDULE_LIMIT:20}
```

含义：

```text
enabled：是否启用定时索引
cron：执行时间，默认每天凌晨 3 点
zone：时区，默认 Asia/Shanghai
limit：每次索引最近多少道题，限制在 1-50
```

### 5. 核心逻辑

定时任务执行：

```text
读取 rag.index-schedule.limit
  ↓
调用 aiRagIndexService.rebuildQuestionIndexBatch(null, limit)
  ↓
批量索引最近题目
  ↓
记录 requested / success / failed 日志
```

核心注释：

```java
// 定时任务只做“最近题目补索引”，真正的 chunk 切分和 embedding 写入仍复用批量索引服务。
```

### 6. 失败处理

定时任务 catch 所有异常，只记录日志：

```java
log.warn("AiRagIndexScheduleException: {}", e.getMessage(), e);
```

原因：

```text
定时索引是后台维护任务，失败不能影响应用主流程。
```

### 7. 本次验证

执行：

```bash
./mvnw -q -DskipTests compile
```

结果：通过。

执行：

```bash
./mvnw -q -Dtest=AiInterviewApplicationTests test
```

结果：通过。

说明：

```text
@EnableScheduling 可以正常加载
AiRagIndexScheduler 默认 disabled，不会在测试启动时触发真实 embedding
配置绑定正常
```

### 8. 当前还没做什么

这个定时任务仍然是第一版。

暂时没做：

```text
分布式锁，避免多实例重复执行
任务执行记录表
失败重试
只索引 update_time 之后变更的题目
任务进度查询
```

这些是生产级任务调度要补的内容。
