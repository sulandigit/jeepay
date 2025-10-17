# Jeepay SkyWalking APM 集成部署文档

> 本文档基于设计文档《引入 SkyWalking 应用性能监控设计文档》实施完成

## 目录

- [1. 概述](#1-概述)
- [2. 快速开始](#2-快速开始)
- [3. 部署方式](#3-部署方式)
- [4. 配置说明](#4-配置说明)
- [5. 功能验证](#5-功能验证)
- [6. 常见问题](#6-常见问题)
- [7. 性能优化](#7-性能优化)
- [8. 运维管理](#8-运维管理)

---

## 1. 概述

### 1.1 集成内容

本次集成为 Jeepay 支付系统引入了 Apache SkyWalking APM 监控能力，包括：

- ✅ **分布式链路追踪**: 跨服务请求调用链路可视化
- ✅ **性能指标监控**: JVM、HTTP、数据库、Redis 等性能指标
- ✅ **日志关联**: 日志中注入 TraceId，便于问题排查
- ✅ **服务拓扑图**: 自动生成服务依赖关系图
- ✅ **告警能力**: 基于指标的智能告警（需配置）

### 1.2 架构说明

```
┌─────────────────────────────────────────────────────────┐
│                    Jeepay 应用层                         │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐   │
│  │ jeepay-      │ │ jeepay-      │ │ jeepay-      │   │
│  │ payment      │ │ manager      │ │ merchant     │   │
│  │ + Agent      │ │ + Agent      │ │ + Agent      │   │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘   │
└─────────┼─────────────────┼─────────────────┼──────────┘
          │                 │                 │
          └─────────────────┴─────────────────┘
                            │
                    ┌───────▼────────┐
                    │  SkyWalking    │
                    │  OAP Server    │
                    │  (11800/12800) │
                    └───────┬────────┘
                            │
                    ┌───────▼────────┐
                    │   SkyWalking   │
                    │   UI (8080)    │
                    └────────────────┘
```

### 1.3 版本信息

| 组件 | 版本 | 说明 |
|------|------|------|
| SkyWalking Agent | 8.16.0 | Java Agent |
| SkyWalking OAP | 9.5.0 | 后端服务 |
| SkyWalking UI | 9.5.0 | 前端界面 |
| Jeepay | 当前版本 | 无需修改业务代码 |

---

## 2. 快速开始

### 2.1 使用 Docker Compose 一键启动

**前提条件:**
- 已安装 Docker 和 Docker Compose
- 确保 8080、11800、12800 端口未被占用

**启动步骤:**

```bash
# 1. 进入项目根目录
cd /data/workspace/jeepay

# 2. 启动所有服务（包括 SkyWalking）
docker-compose up -d

# 3. 查看服务状态
docker-compose ps

# 4. 查看 SkyWalking OAP 日志
docker logs -f jeepay-skywalking-oap

# 5. 查看 SkyWalking UI 日志
docker logs -f jeepay-skywalking-ui
```

**访问地址:**

- **SkyWalking UI**: http://localhost:8080
- **jeepay-payment**: http://localhost:9216
- **jeepay-manager**: http://localhost:9217
- **jeepay-merchant**: http://localhost:9218

### 2.2 验证集成是否成功

1. 访问 SkyWalking UI: http://localhost:8080
2. 等待 1-2 分钟后，在「服务」页面应该看到:
   - `jeepay-payment`
   - `jeepay-manager`
   - `jeepay-merchant`
3. 访问任意 Jeepay 服务接口，触发请求
4. 在 SkyWalking「追踪」页面可以看到请求链路

---

## 3. 部署方式

### 3.1 方式一: 集成到主 docker-compose.yml（推荐）

已在主 `docker-compose.yml` 中集成 SkyWalking 服务，直接使用：

```bash
docker-compose up -d
```

**优点:**
- 一键启动所有服务
- 网络配置自动打通
- 适合开发和测试环境

**涉及的服务:**
- `skywalking-oap`: OAP 服务器
- `skywalking-ui`: Web 界面
- `payment/manager/merchant`: 已集成 Agent

### 3.2 方式二: 独立部署 SkyWalking

如果只想启动 SkyWalking 服务，可使用独立配置:

```bash
cd docker/skywalking
docker-compose -f docker-compose-skywalking.yml up -d
```

然后在应用启动时配置 Agent 参数指向 SkyWalking OAP 地址。

**适用场景:**
- SkyWalking 部署在独立服务器
- 多个应用共享同一个 SkyWalking 集群
- 生产环境推荐

### 3.3 方式三: 本地开发环境（IDEA）

**步骤:**

1. 下载 SkyWalking Agent:
```bash
cd /opt
wget https://archive.apache.org/dist/skywalking/8.16.0/apache-skywalking-java-agent-8.16.0.tgz
tar -zxf apache-skywalking-java-agent-8.16.0.tgz
mv skywalking-agent /opt/skywalking-agent
```

2. 启动 SkyWalking OAP 和 UI:
```bash
cd /data/workspace/jeepay/docker/skywalking
docker-compose -f docker-compose-skywalking.yml up -d
```

3. 在 IDEA 中配置 VM Options:

**jeepay-payment:**
```
-javaagent:/opt/skywalking-agent/skywalking-agent.jar
-DSW_AGENT_NAME=jeepay-payment
-DSW_AGENT_COLLECTOR_BACKEND_SERVICES=127.0.0.1:11800
-DSW_LOGGING_LEVEL=INFO
```

**jeepay-manager:**
```
-javaagent:/opt/skywalking-agent/skywalking-agent.jar
-DSW_AGENT_NAME=jeepay-manager
-DSW_AGENT_COLLECTOR_BACKEND_SERVICES=127.0.0.1:11800
-DSW_LOGGING_LEVEL=INFO
```

**jeepay-merchant:**
```
-javaagent:/opt/skywalking-agent/skywalking-agent.jar
-DSW_AGENT_NAME=jeepay-merchant
-DSW_AGENT_COLLECTOR_BACKEND_SERVICES=127.0.0.1:11800
-DSW_LOGGING_LEVEL=INFO
```

4. 启动应用，查看 SkyWalking UI

---

## 4. 配置说明

### 4.1 Docker Compose 配置

在 `docker-compose.yml` 中，各服务的 SkyWalking 配置:

```yaml
services:
  payment:
    environment:
      JAVA_OPTS: "-javaagent:/opt/skywalking-agent/skywalking-agent.jar"
      SW_AGENT_NAME: jeepay-payment
      SW_AGENT_COLLECTOR_BACKEND_SERVICES: skywalking-oap:11800
      SW_LOGGING_LEVEL: INFO
```

### 4.2 环境变量说明

| 环境变量 | 说明 | 默认值 | 推荐值 |
|----------|------|--------|--------|
| `SW_AGENT_NAME` | 服务名称标识 | - | jeepay-payment/manager/merchant |
| `SW_AGENT_COLLECTOR_BACKEND_SERVICES` | OAP Server 地址 | - | skywalking-oap:11800 |
| `SW_LOGGING_LEVEL` | Agent 日志级别 | INFO | INFO（生产）/ DEBUG（调试） |
| `SW_AGENT_SAMPLE` | 采样率 | -1（全量） | -1（测试）/ 3-10（生产） |
| `SW_LOGGING_DIR` | Agent 日志目录 | - | /opt/skywalking-agent/logs |

### 4.3 日志 TraceId 配置

已在 `logback-spring.xml` 中增加 TraceId 支持:

```xml
<property name="currentLoggerPattern" 
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{tid}] [%logger{15}] - %msg%n" />
```

**日志示例:**
```
2025-10-17 10:30:45.123 INFO  [http-nio-9216-exec-1] [TID:abc123.def456.789] [PaymentController] - 处理支付请求
```

### 4.4 存储配置

**默认配置（H2 内存数据库）:**
- 适用场景: 开发、测试环境
- 优点: 无需额外部署，开箱即用
- 缺点: 重启后数据丢失

**生产环境推荐（ElasticSearch）:**

修改 `docker-compose-skywalking.yml`:

```yaml
skywalking-oap:
  environment:
    SW_STORAGE: elasticsearch
    SW_STORAGE_ES_CLUSTER_NODES: elasticsearch:9200
```

取消注释 ElasticSearch 服务配置。

---

## 5. 功能验证

### 5.1 验证链路追踪

**步骤:**

1. 访问 SkyWalking UI: http://localhost:8080
2. 调用 Jeepay 支付接口:
```bash
curl -X POST http://localhost:9216/api/pay/unifiedOrder \
  -H "Content-Type: application/json" \
  -d '{"mchNo":"M1623984572","appId":"...", ...}'
```
3. 在 SkyWalking UI 点击「追踪」→ 选择服务「jeepay-payment」
4. 可以看到完整的请求链路，包括:
   - HTTP 入口 Span
   - Service 方法调用 Span
   - MySQL 查询 Span
   - Redis 操作 Span
   - 外部 HTTP 调用 Span

### 5.2 验证服务拓扑图

1. 在 SkyWalking UI 点击「拓扑图」
2. 可以看到服务依赖关系:
   - jeepay-payment → MySQL
   - jeepay-payment → Redis
   - jeepay-payment → 支付渠道（外部服务）
   - jeepay-manager → MySQL
   - jeepay-merchant → MySQL

### 5.3 验证性能指标

1. 在 SkyWalking UI 点击「服务」→ 选择「jeepay-payment」
2. 查看性能指标:
   - **响应时间**: P50、P75、P90、P95、P99
   - **吞吐量**: QPM（每分钟请求数）
   - **错误率**: 4xx、5xx 错误统计
   - **JVM 指标**: 堆内存、GC 次数、线程数

### 5.4 验证日志 TraceId

1. 触发一个请求，在 SkyWalking UI 复制 TraceId
2. 查看应用日志:
```bash
docker logs jeepay-payment | grep "TID:abc123.def456.789"
```
3. 可以看到该请求相关的所有日志，便于问题排查

---

## 6. 常见问题

### 6.1 SkyWalking UI 看不到服务

**原因:**
1. Agent 未正确加载
2. OAP Server 地址配置错误
3. 网络不通

**排查步骤:**

1. 检查应用日志，确认 Agent 启动:
```bash
docker logs jeepay-payment 2>&1 | grep -i skywalking
```

应该看到类似输出:
```
[SkyWalking] agent loaded successfully
```

2. 检查环境变量:
```bash
docker exec jeepay-payment env | grep SW_
```

3. 检查网络连通性:
```bash
docker exec jeepay-payment ping -c 3 skywalking-oap
```

4. 查看 OAP Server 日志:
```bash
docker logs jeepay-skywalking-oap | tail -100
```

### 6.2 性能下降明显

**原因:**
- 全量采样（`SW_AGENT_SAMPLE=-1`）在高并发下会有性能开销

**解决方案:**

1. 调整采样率（生产环境推荐）:
```yaml
environment:
  SW_AGENT_SAMPLE: 5  # 每3秒采样5条链路
```

2. 禁用不需要的插件:
```yaml
environment:
  SW_PLUGIN_EXCLUDE: spring-webflux-5.x-plugin,dubbo-plugin
```

### 6.3 日志中没有 TraceId

**原因:**
- Logback 配置未生效
- 使用了其他日志框架

**解决方案:**

1. 确认使用的是 `logback-spring.xml`
2. 检查日志格式是否包含 `%X{tid}`
3. 重启应用使配置生效

### 6.4 OAP Server 内存占用高

**原因:**
- H2 内存数据库积累大量数据
- 默认保留时间过长

**解决方案:**

1. 切换到 ElasticSearch 存储
2. 调整数据保留时间:
```yaml
skywalking-oap:
  environment:
    SW_CORE_RECORD_DATA_TTL: 3  # 保留3天
    SW_CORE_METRICS_DATA_TTL: 7  # 指标保留7天
```

---

## 7. 性能优化

### 7.1 采样策略

**开发/测试环境:**
```yaml
SW_AGENT_SAMPLE: -1  # 全量采集
```

**生产环境（QPS < 1000）:**
```yaml
SW_AGENT_SAMPLE: 10  # 每3秒采样10条
```

**生产环境（QPS > 1000）:**
```yaml
SW_AGENT_SAMPLE: 5  # 每3秒采样5条
```

### 7.2 插件优化

禁用不需要的插件以降低开销:

```yaml
environment:
  SW_PLUGIN_EXCLUDE: spring-webflux-5.x-plugin,kafka-plugin,dubbo-plugin
```

### 7.3 JVM 参数优化

为 OAP Server 分配足够内存:

```yaml
skywalking-oap:
  environment:
    JAVA_OPTS: "-Xms1g -Xmx2g -XX:+UseG1GC"
```

### 7.4 批量上报优化

Agent 默认使用批量异步上报，无需额外配置。如需调整:

```yaml
environment:
  SW_AGENT_BUFFER_CHANNEL_SIZE: 5000
  SW_AGENT_BUFFER_SIZE: 300
```

---

## 8. 运维管理

### 8.1 日志管理

**Agent 日志位置:**
- Docker 容器内: `/opt/skywalking-agent/logs/`
- 日志级别: 通过 `SW_LOGGING_LEVEL` 控制

**OAP Server 日志:**
```bash
docker logs -f jeepay-skywalking-oap
```

**UI 日志:**
```bash
docker logs -f jeepay-skywalking-ui
```

### 8.2 数据备份

**H2 数据库备份（测试环境）:**
```bash
docker cp jeepay-skywalking-oap:/skywalking/data ./skywalking-backup
```

**ElasticSearch 备份（生产环境）:**
使用 ES 官方快照功能。

### 8.3 监控指标

**关键指标:**
1. OAP Server CPU/内存使用率
2. Agent 数据上报成功率
3. 链路数据存储大小
4. UI 访问响应时间

**Prometheus 集成（可选）:**

OAP Server 暴露 Prometheus 指标:
```
http://skywalking-oap:1234/metrics
```

### 8.4 升级指南

**升级 Agent:**
1. 修改 Dockerfile 中的 `SKYWALKING_VERSION`
2. 重新构建镜像: `docker-compose build payment manager merchant`
3. 重启服务: `docker-compose up -d`

**升级 OAP/UI:**
1. 修改 `docker-compose.yml` 中的镜像版本
2. 重启服务: `docker-compose up -d skywalking-oap skywalking-ui`

---

## 9. 生产环境部署建议

### 9.1 高可用架构

```yaml
services:
  skywalking-oap-1:
    ...
  skywalking-oap-2:
    ...
  nginx:
    # 负载均衡到多个 OAP
    ...
  elasticsearch:
    # ES 集群
    ...
```

### 9.2 资源规划

**日均链路数 < 100万:**
- OAP Server: 2核4GB × 1台
- ElasticSearch: 4核8GB × 1台

**日均链路数 100万-500万:**
- OAP Server: 4核8GB × 2台
- ElasticSearch: 4核16GB × 3台

### 9.3 安全加固

1. 启用 OAP 认证:
```yaml
SW_CORE_AUTHENTICATION: true
SW_CORE_AUTHENTICATION_TOKEN: your-secret-token
```

2. 限制 UI 访问（使用 Nginx 反向代理）
3. 配置网络隔离（仅允许应用访问 OAP）

---

## 10. 参考资料

- [SkyWalking 官方文档](https://skywalking.apache.org/docs/)
- [Jeepay 设计文档: 引入 SkyWalking 应用性能监控设计文档](../docs/skywalking-design.md)
- [SkyWalking Agent 配置参考](https://skywalking.apache.org/docs/skywalking-java/latest/en/setup/service-agent/java-agent/configurations/)

---

## 11. 联系支持

如遇到问题，请:
1. 查看本文档「常见问题」章节
2. 查看应用和 OAP 日志
3. 提交 Issue 到 Jeepay 项目仓库
