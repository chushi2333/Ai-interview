# Java Backend Template

一个最小化 Spring Boot 后端模板，适合作为新 Java Web 项目的起点。

## 当前包含

- Spring Boot Web
- 参数校验
- 统一响应结构 `Response<T>`
- 全局异常处理
- Swagger / OpenAPI
- 健康检查接口
- Lombok
- JUnit 基础测试

## 当前不包含

- 用户登录
- JWT 鉴权
- Redis
- RabbitMQ
- MySQL / MyBatis
- 具体业务模块

这些能力建议按具体项目需要再单独引入，避免模板过重。

## 启动

```bash
mvn spring-boot:run
```

默认端口：`8080`

健康检查：

```http
GET /api/health
```

Swagger UI：

```text
http://localhost:8080/swagger-ui.html
```

## 配置

常用环境变量：

- `SERVER_PORT`：服务端口，默认 `8080`
- `SPRING_PROFILES_ACTIVE`：运行环境，默认 `dev`
- `APP_LOG_LEVEL`：应用日志级别，默认 `INFO`

## 项目结构

```text
src/main/java/com/chushi/template
├── commons/vo/Response.java
├── configurations/OpenApiConfiguration.java
├── controller/BaseController.java
├── controller/HealthController.java
├── exceptions/BusinessException.java
├── exceptions/GlobalExceptionHandler.java
└── JavaTemplateApplication.java
```
