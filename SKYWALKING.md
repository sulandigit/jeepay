# SkyWalking APM 集成说明

本次已成功为 Jeepay 支付系统集成 Apache SkyWalking 应用性能监控（APM）。

## 🎯 集成内容

### ✅ 已完成的工作

1. **Docker 配置集成**
   - 在主 `docker-compose.yml` 中添加 SkyWalking OAP 和 UI 服务
   - 为 `jeepay-payment`、`jeepay-manager`、`jeepay-merchant` 三个服务配置 Agent

2. **Dockerfile 改造**
   - 在各服务 Dockerfile 中集成 SkyWalking Agent 8.16.0
   - 支持通过环境变量动态配置 Agent 参数

3. **日志增强**
   - 在所有服务的 `logback-spring.xml` 中添加 TraceId 支持
   - 日志格式包含 `[%X{tid}]` 占位符，便于链路关联

4. **配置文件**
   - 创建独立的 SkyWalking Docker Compose 配置
   - 提供环境变量配置模板（`.env.example`）
   - 提供本地开发启动脚本（`local-dev-setup.sh`）

5. **文档**
   - 完整的部署文档（`docs/SKYWALKING_DEPLOYMENT.md`）
   - 详细的使用指南（`docs/SKYWALKING_USAGE.md`）
   - Agent 配置说明（`docker/skywalking/README.md`）

## 📁 新增文件列表

```
jeepay/
├── docker-compose.yml                          # 已更新：添加 SkyWalking 服务
├── docker/
│   └── skywalking/
│       ├── Dockerfile                          # SkyWalking Agent 基础镜像
│       ├── agent-config.properties             # Agent 配置模板
│       ├── docker-compose-skywalking.yml       # 独立部署配置
│       ├── local-dev-setup.sh                  # 本地开发脚本
│       ├── .env.example                        # 环境变量示例
│       └── README.md                           # Agent 配置说明
├── docs/
│   ├── SKYWALKING_DEPLOYMENT.md                # 部署文档
│   └── SKYWALKING_USAGE.md                     # 使用指南
├── jeepay-payment/
│   ├── Dockerfile                              # 已更新：集成 Agent
│   └── src/main/resources/
│       └── logback-spring.xml                  # 已更新：TraceId 支持
├── jeepay-manager/
│   ├── Dockerfile                              # 已更新：集成 Agent
│   └── src/main/resources/
│       └── logback-spring.xml                  # 已更新：TraceId 支持
└── jeepay-merchant/
    ├── Dockerfile                              # 已更新：集成 Agent
    └── src/main/resources/
        └── logback-spring.xml                  # 已更新：TraceId 支持
```

## 🚀 快速开始

### 方式一: Docker Compose 一键启动（推荐）

```bash
# 启动所有服务（包括 SkyWalking）
docker-compose up -d

# 访问 SkyWalking UI
open http://localhost:8080
```

### 方式二: 本地开发环境

1. 启动 SkyWalking OAP 和 UI:
```bash
cd docker/skywalking
docker-compose -f docker-compose-skywalking.yml up -d
```

2. 在 IDEA 中配置 VM Options（以 jeepay-payment 为例）:
```
-javaagent:/opt/skywalking-agent/skywalking-agent.jar
-DSW_AGENT_NAME=jeepay-payment
-DSW_AGENT_COLLECTOR_BACKEND_SERVICES=127.0.0.1:11800
```

详细步骤请参考: [docs/SKYWALKING_DEPLOYMENT.md](docs/SKYWALKING_DEPLOYMENT.md)

## 📊 功能特性

### 1. 分布式链路追踪

- ✅ 自动追踪 HTTP 请求
- ✅ 自动追踪数据库查询（MySQL）
- ✅ 自动追踪 Redis 操作
- ✅ 自动追踪消息队列（ActiveMQ/RabbitMQ）
- ✅ 支持跨服务链路传递

### 2. 性能指标监控

- ✅ JVM 指标（堆内存、GC、线程）
- ✅ HTTP 指标（响应时间、吞吐量、错误率）
- ✅ 数据库指标（SQL 执行时间、慢查询）
- ✅ Redis 指标（命令执行时间）

### 3. 服务拓扑图

- ✅ 自动生成服务依赖关系图
- ✅ 实时展示调用关系和健康状态

### 4. 日志关联

- ✅ 日志中自动注入 TraceId
- ✅ 支持通过 TraceId 关联所有日志

## 🔧 配置说明

### 环境变量

各服务已配置以下 SkyWalking 环境变量:

| 服务 | SW_AGENT_NAME | 端口 | 说明 |
|------|---------------|------|------|
| jeepay-payment | `jeepay-payment` | 9216 | 支付网关 |
| jeepay-manager | `jeepay-manager` | 9217 | 运营平台 |
| jeepay-merchant | `jeepay-merchant` | 9218 | 商户平台 |

### 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| SkyWalking UI | http://localhost:8080 | 监控界面 |
| OAP gRPC | localhost:11800 | Agent 上报端口 |
| OAP HTTP | localhost:12800 | UI 查询端口 |

## 📖 文档导航

- **[部署文档](docs/SKYWALKING_DEPLOYMENT.md)** - 详细的部署步骤、配置说明、常见问题
- **[使用指南](docs/SKYWALKING_USAGE.md)** - 功能使用、链路追踪、性能分析、告警配置
- **[Agent 配置](docker/skywalking/README.md)** - Agent 参数说明、插件配置

## ⚙️ 核心配置项

### 采样率配置

```yaml
# 开发/测试环境：全量采集
SW_AGENT_SAMPLE: -1

# 生产环境：按需采样（每3秒采样N条）
SW_AGENT_SAMPLE: 5
```

### 日志级别

```yaml
# Agent 日志级别
SW_LOGGING_LEVEL: INFO  # DEBUG, INFO, WARN, ERROR
```

### 存储配置

默认使用 H2 内存数据库（适合测试），生产环境建议使用 ElasticSearch。

修改 `docker-compose.yml`:
```yaml
skywalking-oap:
  environment:
    SW_STORAGE: elasticsearch
    SW_STORAGE_ES_CLUSTER_NODES: elasticsearch:9200
```

## 🎯 使用示例

### 1. 查看服务列表

访问 http://localhost:8080，可以看到三个服务:
- jeepay-payment
- jeepay-manager
- jeepay-merchant

### 2. 查看链路追踪

1. 点击「追踪」菜单
2. 选择服务 `jeepay-payment`
3. 触发支付请求后可以看到完整链路

### 3. 通过 TraceId 排查问题

```bash
# 1. 从 SkyWalking UI 复制 TraceId
# 2. 在日志中搜索
docker logs jeepay-payment | grep "TID:abc123.def456.789"
```

## ⚠️ 注意事项

### 性能影响

- **CPU 开销**: +3%-5%
- **内存开销**: +50-100MB
- **响应时间**: +0.5-2ms

### 生产环境优化

1. **调整采样率**: `SW_AGENT_SAMPLE: 5`（每3秒采样5条）
2. **使用 ElasticSearch**: 替换 H2 内存数据库
3. **配置告警规则**: 及时发现性能问题
4. **定期清理数据**: 避免存储空间不足

## 🔍 故障排查

### SkyWalking UI 看不到服务

```bash
# 1. 检查 Agent 是否加载
docker logs jeepay-payment 2>&1 | grep -i skywalking

# 2. 检查环境变量
docker exec jeepay-payment env | grep SW_

# 3. 检查网络连通性
docker exec jeepay-payment ping -c 3 skywalking-oap

# 4. 查看 OAP 日志
docker logs jeepay-skywalking-oap
```

更多问题请参考: [docs/SKYWALKING_DEPLOYMENT.md#6-常见问题](docs/SKYWALKING_DEPLOYMENT.md#6-常见问题)

## 📈 监控指标示例

访问 SkyWalking UI 后可以看到:

**服务性能指标:**
- 响应时间: P50: 50ms, P95: 200ms, P99: 500ms
- 吞吐量: 120 QPM
- 错误率: 0.5%

**JVM 指标:**
- 堆内存: 1.2GB / 2GB
- Young GC: 10次/小时, 平均 50ms
- Full GC: 0次

**数据库指标:**
- 平均查询时间: 10ms
- 慢查询（>100ms）: 5个

## 🛠️ 下一步建议

1. **配置告警规则** - 在 `alarm-settings.yml` 中配置告警
2. **集成 Prometheus** - 导出指标到 Prometheus
3. **切换 ElasticSearch** - 生产环境使用 ES 存储
4. **自定义业务指标** - 使用 Meter API 上报业务数据

## 📞 技术支持

- SkyWalking 官方文档: https://skywalking.apache.org/docs/
- Jeepay 项目文档: [docs/](docs/)
- 问题反馈: 提交 Issue 到项目仓库

---

**版本信息:**
- SkyWalking Agent: 8.16.0
- SkyWalking OAP: 9.5.0
- SkyWalking UI: 9.5.0
- 集成日期: 2025-10-17
