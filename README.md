# Ai Interview

一个以学习和工程实践为主的智能面试平台后端项目。

当前项目不是单点功能练习，而是按真实后端项目思路逐步搭建完整能力。现阶段已经覆盖：

- 官方题目与题库管理
- 用户头像与题库封面对象存储
- 题目详情与题库详情缓存
- 基于 RabbitMQ 的 Elasticsearch 异步同步
- 题目搜索能力
- 用户签到、单选自测与学习行为留痕

## 技术栈

- Java 17
- Spring Boot 3
- MyBatis
- MySQL
- Redis
- RabbitMQ
- Elasticsearch
- SeaweedFS
- Flyway
- Docker Compose

## 项目目标

项目当前主要关注两件事：

- 通过题目、题库、搜索等业务模块练习完整后端开发流程
- 通过 Redis、RabbitMQ、SeaweedFS、Elasticsearch 等基础设施训练真实项目的架构思维

整体思路不是一次把所有功能做满，而是先把主链路跑通，再逐步扩展搜索、用户行为和后台管理能力。

## 核心业务

- `Question`
  题目主数据
- `QuestionBank`
  题库主数据
- `QuestionBankQuestion`
  题库和题目的关联关系

### 权限模型

- `USER`
  普通用户
- `ADMIN`
  会员用户
- `SUPER_ADMIN`
  内容管理员

### 权限规则

- 题目、题库、题库题目关系的写操作只允许 `SUPER_ADMIN`
- 题目列表、题库列表、题库详情、题库下题目列表对所有登录用户开放
- 题目详情接口对所有登录用户开放，但会员题详情只允许 `ADMIN` 和 `SUPER_ADMIN` 查看

## 架构设计

### 1. 主数据层

- MySQL 负责存储题目、题库、用户等主业务数据
- Flyway 负责管理数据库迁移脚本

### 2. 文件存储层

- SeaweedFS 负责对象存储
- 当前已接入：
  - 用户头像上传
  - 题库封面上传

### 3. 缓存层

- Redis 负责详情缓存
- 当前已缓存：
  - 题目详情
  - 题库详情

项目当前只做详情缓存，不做列表缓存，原因是详情属于典型的多读少写场景，而列表缓存更容易带来较高的内存占用和复杂的失效成本。

### 4. 搜索层

- Elasticsearch 负责题目搜索
- MySQL 是主库
- Elasticsearch 不是主数据源，而是搜索视图

当前搜索支持：

- 关键词搜索
- 难度过滤
- 单标签精确过滤
- 基础排序

### 5. 异步同步层

- RabbitMQ 负责将 MySQL 变更异步同步到 Elasticsearch
- 当前链路为：

`MySQL -> RabbitMQ -> Elasticsearch`

设计思想：

- 先写 MySQL，保证主业务成功
- 事务提交后再发消息，避免数据库回滚但 ES 已更新的问题
- 消费者异步更新 Elasticsearch，降低主链路对搜索引擎的耦合

### 6. 死信队列

题目同步 Elasticsearch 的消息链路已配置死信队列。

作用：

- 保留消费失败的消息
- 方便排查同步异常
- 为后续补偿和重试提供入口

### 7. 学习行为层

当前学习行为能力按三层拆分：

- `question_view_record`
  记录用户看过哪些题，解决“学过什么”
- `question_self_test_record`
  记录用户做过哪些自测题及结果，解决“会不会”
- `question_practice_record`
  作为统计投影层，服务热力图、今日练习次数和年度聚合

当前自测题只支持单选题。

## 分支策略

- `main`
  保持为偏通用的 Java 项目模板分支
- `dev`
  作为当前业务主线开发分支
- `feature/question-bank`
  题库模块、对象存储、详情缓存
- `feature/question-search-es`
  基于 `feature/question-bank` 继续扩展 Elasticsearch 搜索能力
- `feature/user-study-behavior`
  基于 `dev` 继续扩展签到、自测和学习行为记录能力

## 当前分支进度

### `feature/question-bank`

已完成：

- 题目、题库、题库题目关系模块
- 用户头像和题库封面对象存储
- 题目详情缓存
- 题库详情缓存
- Docker 开发环境
- Flyway 初始化

### `feature/question-search-es`

基于 `feature/question-bank` 继续开发。

已完成：

- Elasticsearch 接入
- RabbitMQ 异步同步链路
- 死信队列配置
- 题目搜索接口
- 难度过滤
- 单标签精确过滤

### `feature/user-study-behavior`

已完成：

- 用户签到
  - Redis Bitmap 年度签到记录
  - 今日是否签到、连续签到、本月签到天数、总签到天数统计
- 单选自测题
  - 管理员创建题目下的单选自测题
  - 用户查看自测题并提交答案
  - 系统直接返回对错、正确答案和解析
- 学习行为记录
  - 题目详情自动写入看题记录
  - 自测提交写入 `question_self_test_record`
  - 练习统计继续通过 `question_practice_record` 输出
- 年度热力图友好统计
  - 看题记录和练习记录都会补齐全年空日期，前端可直接画热力图

## 开发环境

项目支持使用 Docker Compose 启动基础依赖环境：

- MySQL
- Redis
- RabbitMQ
- Elasticsearch
- SeaweedFS

启动方式参考：

- [docker-compose.dev.yml](./docker-compose.dev.yml)
- [docs/docker-dev.md](./docs/docker-dev.md)

## 当前阶段总结

项目当前已经形成一条比较完整的后端主链路：

- MySQL 存储主业务数据
- Redis 缓存高频详情数据
- SeaweedFS 负责文件上传
- RabbitMQ 解耦 Elasticsearch 同步
- Elasticsearch 承担题目搜索能力

这一阶段的重点已经从“能不能把接口写出来”转到了“怎么把业务数据、缓存、文件和搜索组织成一套可持续扩展的后端结构”。

## 后续规划

- 中文分词优化
- 搜索容错模糊能力
- 多标签过滤
- 搜索结果高亮
- 用户刷题记录
- 做题历史与用户侧学习行为能力
- 更细的学习停留时长判断

## 学习行为说明

当前“看题记录”采用的是基础自动留痕策略：

- 用户成功打开题目详情，就记一次查看行为
- 同一天同一用户同一题只记一条，避免刷新页面刷高热度

后续如果要把“看满 60 秒才算有效学习”这种规则做严谨，不适合继续放在当前自动留痕链路里，更合理的方式是：

- 前端达到停留阈值后主动上报
- 后端再做幂等和频控

这样可以把“打开过题目”和“有效学习过题目”两类语义分开。
