# SkyWalking 使用指南

## 快速索引

- [查看服务列表](#查看服务列表)
- [查看链路追踪](#查看链路追踪)
- [查看服务拓扑](#查看服务拓扑)
- [查看性能指标](#查看性能指标)
- [通过 TraceId 排查问题](#通过-traceid-排查问题)
- [配置告警规则](#配置告警规则)

---

## 查看服务列表

1. 访问 SkyWalking UI: http://localhost:8080
2. 默认进入「服务」页面
3. 可以看到所有已注册的服务:
   - `jeepay-payment` (支付网关)
   - `jeepay-manager` (运营平台)
   - `jeepay-merchant` (商户平台)

**服务状态指标:**
- 健康状态（绿色/红色）
- 响应时间（平均、P95、P99）
- 吞吐量（QPM）
- 错误率

---

## 查看链路追踪

### 方法一: 通过追踪页面

1. 点击顶部菜单「追踪」
2. 选择时间范围（默认最近15分钟）
3. 选择服务: `jeepay-payment`
4. 可选: 添加过滤条件
   - 端点（Endpoint）
   - 状态（成功/失败）
   - 最小/最大耗时
5. 点击任一链路查看详情

### 方法二: 通过服务页面

1. 在「服务」页面点击某个服务（如 `jeepay-payment`）
2. 切换到「追踪」Tab
3. 查看该服务相关的所有链路

### 链路详情解读

**Span 列表:**
```
[Entry] /api/pay/unifiedOrder              200ms
  ├─ [Local] PaymentService.createOrder    150ms
  │   ├─ [Exit] MySQL: SELECT ...           10ms
  │   ├─ [Exit] Redis: GET ...               5ms
  │   └─ [Exit] HTTP: 支付渠道调用          120ms
  └─ [Exit] MySQL: INSERT ...                30ms
```

**关键信息:**
- **Entry Span**: HTTP 入口请求
- **Local Span**: 本地方法调用
- **Exit Span**: 外部调用（数据库、缓存、HTTP）
- **耗时分析**: 每个 Span 的执行时间
- **异常信息**: 红色标记的 Span 表示有异常

---

## 查看服务拓扑

1. 点击顶部菜单「拓扑图」
2. 可以看到服务依赖关系图:

```
┌──────────────┐
│  用户请求     │
└──────┬───────┘
       │
┌──────▼──────────┐
│ jeepay-payment  │
└──────┬──────────┘
       │
       ├─────────┐
       │         │
   ┌───▼──┐  ┌──▼────┐  ┌────────┐
   │ MySQL│  │ Redis │  │ 支付渠道│
   └──────┘  └───────┘  └────────┘
```

**拓扑图功能:**
- 箭头方向表示调用方向
- 箭头颜色表示健康状态（绿色正常/红色异常）
- 点击节点查看该服务详情
- 点击连线查看调用链路

---

## 查看性能指标

### JVM 指标

1. 点击「服务」→ 选择服务 → 「JVM」Tab
2. 可以看到:
   - **堆内存**: 已用/最大值，Young/Old/Metaspace
   - **GC 统计**: Young GC/Full GC 次数和耗时
   - **线程数**: 活动线程/峰值线程
   - **CPU 使用率**: 进程 CPU 占用

### HTTP 指标

1. 「服务」→ 「端点」Tab
2. 可以看到所有接口的性能:
   - 响应时间（P50/P75/P90/P95/P99）
   - 吞吐量（QPM）
   - 错误率

**示例:**
```
端点: POST /api/pay/unifiedOrder
- P95 响应时间: 350ms
- P99 响应时间: 800ms
- QPM: 120
- 错误率: 0.5%
```

### 数据库指标

1. 「数据库」菜单 → 选择数据库实例
2. 查看:
   - SQL 执行时间分布
   - 慢查询 TOP10
   - 连接池状态

---

## 通过 TraceId 排查问题

### 场景: 用户反馈支付失败

**步骤 1: 获取 TraceId**

从用户或日志中获取 TraceId，格式类似:
```
TID:abc123.def456.789
```

**步骤 2: 在 SkyWalking 中搜索**

1. 点击「追踪」菜单
2. 在「Trace ID」输入框中输入: `abc123.def456.789`
3. 点击搜索

**步骤 3: 分析链路**

查看链路详情，找到失败的 Span:
- 红色 Span 表示有异常
- 点击 Span 查看异常堆栈
- 查看每个 Span 的耗时，定位慢操作

**步骤 4: 查看关联日志**

1. 复制 TraceId: `abc123.def456.789`
2. 在日志系统中搜索:
```bash
# Docker 容器日志
docker logs jeepay-payment | grep "abc123.def456.789"

# 或在 Kibana/ELK 中搜索
tid: "abc123.def456.789"
```
3. 查看完整的业务日志

**步骤 5: 定位问题**

通过链路和日志结合分析:
- SQL 慢查询导致超时
- 支付渠道返回异常
- 参数校验失败
- 系统资源不足

---

## 配置告警规则

### 内置告警规则

SkyWalking 提供了默认告警规则，位于 OAP 配置中。

### 自定义告警规则

**示例: 配置支付成功率告警**

1. 创建告警配置文件 `alarm-settings.yml`:

```yaml
rules:
  # 服务响应时间告警
  service_resp_time_rule:
    metrics-name: service_resp_time
    op: ">"
    threshold: 3000  # 3秒
    period: 5  # 5分钟
    count: 3  # 连续3次
    message: "服务 {name} 响应时间超过3秒"
    
  # 服务错误率告警
  service_sla_rule:
    metrics-name: service_sla
    op: "<"
    threshold: 95  # 成功率低于95%
    period: 5
    count: 2
    message: "服务 {name} 成功率低于95%"
    
  # 端点响应时间告警
  endpoint_resp_time_rule:
    metrics-name: endpoint_resp_time
    op: ">"
    threshold: 5000  # 5秒
    period: 5
    count: 3
    message: "端点 {name} 响应时间超过5秒"

webhooks:
  - http://your-webhook-url/alert  # 钉钉/企业微信 Webhook
```

2. 挂载配置到 OAP:

```yaml
skywalking-oap:
  volumes:
    - ./alarm-settings.yml:/skywalking/config/alarm-settings.yml
```

3. 重启 OAP 服务:
```bash
docker-compose restart skywalking-oap
```

### 告警通知方式

**方法一: Webhook 通知**

在 `alarm-settings.yml` 中配置 Webhook URL，接收告警 JSON:

```json
{
  "scopeId": 1,
  "name": "jeepay-payment",
  "id0": "jeepay-payment",
  "ruleName": "service_resp_time_rule",
  "alarmMessage": "服务 jeepay-payment 响应时间超过3秒",
  "startTime": 1634567890000
}
```

**方法二: 钉钉机器人**

通过 Webhook 转发到钉钉群:

```yaml
webhooks:
  - http://your-server/dingtalk-webhook-proxy
```

在代理服务中转换格式并发送到钉钉。

---

## 高级功能

### 自定义 Span

如需追踪特定业务逻辑，可以手动创建 Span:

```java
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

@Service
public class PaymentService {
    
    @Trace  // 自动创建 Span
    public void processPayment(PayOrder order) {
        // 获取 TraceId
        String traceId = TraceContext.traceId();
        log.info("Processing payment, TraceId: {}", traceId);
        
        // 业务逻辑
        ...
    }
}
```

### 自定义指标

使用 SkyWalking Meter API 上报业务指标:

```java
import org.apache.skywalking.apm.toolkit.meter.*;

MeterFactory.counter("pay_order_count")
    .tag("channel", "alipay")
    .build()
    .increment(1);

MeterFactory.histogram("pay_amount_histogram")
    .tag("channel", "wechat")
    .build()
    .addValue(orderAmount);
```

---

## 性能调优建议

### 采样策略

**开发环境:**
```yaml
SW_AGENT_SAMPLE: -1  # 全量采集，便于调试
```

**测试环境:**
```yaml
SW_AGENT_SAMPLE: -1  # 全量采集，验证功能
```

**生产环境（QPS < 1000）:**
```yaml
SW_AGENT_SAMPLE: 10  # 每3秒采样10条
```

**生产环境（QPS > 1000）:**
```yaml
SW_AGENT_SAMPLE: 5  # 每3秒采样5条
```

### 忽略静态资源

避免采集静态资源请求:

```yaml
environment:
  SW_AGENT_IGNORE_SUFFIX: .jpg,.jpeg,.js,.css,.png,.bmp,.gif,.ico,.mp3,.mp4,.html,.svg
```

### 禁用不需要的插件

如果不使用某些组件，可以禁用对应插件:

```yaml
environment:
  SW_PLUGIN_EXCLUDE: kafka-plugin,dubbo-plugin,spring-webflux-5.x-plugin
```

---

## 常见使用场景

### 场景 1: 接口响应慢排查

1. 在「端点」页面找到慢接口
2. 点击查看链路详情
3. 分析各 Span 耗时占比
4. 定位瓶颈:
   - 数据库慢查询 → 优化 SQL 或添加索引
   - 外部调用慢 → 检查网络或下游服务
   - 本地计算慢 → 优化算法或增加缓存

### 场景 2: 支付成功率下降排查

1. 在「服务」页面查看 `jeepay-payment` 错误率
2. 点击「追踪」→ 筛选「状态:Error」
3. 查看失败链路的共同特征:
   - 特定支付渠道异常
   - 数据库连接超时
   - 参数校验失败
4. 通过 TraceId 查看详细日志
5. 修复问题并验证

### 场景 3: 系统性能评估

1. 选择时间范围（如最近1小时）
2. 查看各服务的:
   - 平均响应时间
   - P95/P99 响应时间
   - 吞吐量（QPM）
   - 错误率
3. 查看 JVM 指标:
   - 内存使用趋势
   - GC 频率和耗时
   - 线程数变化
4. 评估是否需要扩容或优化

---

## 参考资料

- [SkyWalking UI 使用文档](https://skywalking.apache.org/docs/main/latest/en/ui/readme/)
- [告警规则配置](https://skywalking.apache.org/docs/main/latest/en/setup/backend/backend-alarm/)
- [Meter System](https://skywalking.apache.org/docs/main/latest/en/setup/service-agent/java-agent/java-plugin-development-guide/)
