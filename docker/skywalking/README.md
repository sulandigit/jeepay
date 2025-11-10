# SkyWalking Agent 配置说明

## 目录说明

- `Dockerfile`: SkyWalking Agent 基础镜像构建文件
- `agent-config.properties`: Agent 配置文件模板
- `README.md`: 本文档

## Agent 版本

当前使用的 SkyWalking Agent 版本：**8.16.0**

## 环境变量配置

在 Docker Compose 或 Dockerfile 中通过环境变量配置 Agent：

### 必需配置

- `SW_AGENT_NAME`: 服务名称（如：jeepay-payment、jeepay-manager、jeepay-merchant）
- `SW_AGENT_COLLECTOR_BACKEND_SERVICES`: OAP Server 地址（默认：skywalking-oap:11800）

### 可选配置

- `SW_LOGGING_LEVEL`: Agent 日志级别（默认：INFO）
- `SW_LOGGING_DIR`: Agent 日志目录（默认：/opt/skywalking-agent/logs）
- `SW_AGENT_SAMPLE`: 采样率，-1表示全量采集（默认：-1）
- `SW_PLUGIN_EXCLUDE`: 禁用的插件列表

## 使用示例

在 Dockerfile 中启用 SkyWalking Agent：

```dockerfile
ENV JAVA_OPTS="-javaagent:/opt/skywalking-agent/skywalking-agent.jar"
ENV SW_AGENT_NAME=jeepay-payment
ENV SW_AGENT_COLLECTOR_BACKEND_SERVICES=skywalking-oap:11800
```

## 注意事项

1. Agent 会自动插桩 Spring Boot、MyBatis、Redis、MySQL 等组件
2. 建议生产环境调整采样率以降低性能开销
3. Agent 日志默认输出到 `/opt/skywalking-agent/logs` 目录
4. 如遇到兼容性问题，可通过 `SW_PLUGIN_EXCLUDE` 禁用特定插件
