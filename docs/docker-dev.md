# Docker 开发环境说明

## 目标

这份配置的目的不是把所有东西都装到本机，而是把开发依赖环境放进 Docker。

当前纳入 Docker 的服务：

- MySQL
- Redis
- RabbitMQ

应用本身仍然可以在本机直接运行，这样调试最方便。

## 先理解 4 个概念

### 1. Image

镜像就是模板。

例如：

- `mysql:8.4`
- `redis:7.4`
- `rabbitmq:3.13-management`

### 2. Container

容器就是镜像跑起来后的实例。

例如：

- `ai-interview-mysql`
- `ai-interview-redis`

### 3. Volume

卷用来持久化数据。

否则你把容器删掉，数据库数据也会一起丢。

### 4. Docker Compose

Compose 用来一次性管理多个服务。

你的项目现在不是只依赖一个数据库，而是依赖多套环境，所以用 `docker compose` 最合适。

## 当前文件

- `docker-compose.dev.yml`
- `src/main/resources/application-dev.yaml`

其中：

- `docker-compose.dev.yml` 负责启动依赖环境
- `application-dev.yaml` 改成了优先读取环境变量，没有环境变量时再回退到本地默认值

## 最常用命令

### 启动环境

```bash
docker compose -f docker-compose.dev.yml up -d
```

### 查看容器状态

```bash
docker compose -f docker-compose.dev.yml ps
```

### 查看日志

```bash
docker compose -f docker-compose.dev.yml logs -f
```

如果只看某个服务：

```bash
docker compose -f docker-compose.dev.yml logs -f mysql
```

### 停止环境

```bash
docker compose -f docker-compose.dev.yml down
```

### 停止并删除数据卷

```bash
docker compose -f docker-compose.dev.yml down -v
```

这个命令会清空 MySQL、Redis、RabbitMQ 的数据，谨慎使用。

## 服务访问地址

### MySQL

- Host: `localhost`
- Port: `3306`
- Database: `interview`
- Username: `root`
- Password: `123456`

### Redis

- Host: `localhost`
- Port: `6379`

### RabbitMQ

- AMQP Port: `5672`
- Console: `http://localhost:15672`
- Username: `guest`
- Password: `guest`

## 当前开发流程建议

### 1. 先启动依赖环境

```bash
docker compose -f docker-compose.dev.yml up -d
```

### 2. 再在本机启动 Spring Boot

```bash
mvn spring-boot:run
```

或者用 IDE 直接启动。

### 3. 应用连接 Docker 里的依赖服务

因为端口映射到了本机，所以应用本地运行时仍然用：

- `localhost:3306`
- `localhost:6379`
- `localhost:5672`

## 为什么先不把应用本身也放进 Docker

因为你当前阶段更重视开发效率。

如果把应用本身也塞进容器：

- 热更新更麻烦
- 调试更麻烦
- 改 Java 代码的反馈速度更慢

所以更合理的是：

- 依赖环境 Docker 化
- 应用本机运行

等以后你要部署，再补应用自己的 `Dockerfile`。

## 下一步

当前 Docker 环境主要是给后面这几块准备的：

- 缓存
- Elasticsearch
对象存储后面会单独按 SeaweedFS 方案接入，不在当前这份开发环境里提前放。
