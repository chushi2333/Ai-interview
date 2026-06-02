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
