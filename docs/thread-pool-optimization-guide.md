# JeePay线程池配置优化使用文档

## 概述

本文档详细介绍了JeePay系统线程池配置优化方案的使用方法、配置说明和最佳实践。基于统一线程池管理框架，提供了完整的线程池创建、配置、监控和管理解决方案。

## 目录

1. [快速开始](#快速开始)
2. [配置说明](#配置说明)
3. [使用指南](#使用指南)
4. [监控与告警](#监控与告警)
5. [最佳实践](#最佳实践)
6. [故障排查](#故障排查)
7. [性能调优](#性能调优)

## 快速开始

### 1. 启用线程池管理

在应用配置文件中启用线程池统一管理：

```yaml
jeepay:
  thread-pool:
    enabled: true
```

### 2. 基础配置

配置全局默认参数：

```yaml
jeepay:
  thread-pool:
    enabled: true
    global:
      core-pool-size: 5
      max-pool-size: 20
      queue-capacity: 100
      keep-alive-seconds: 300
      rejected-execution-handler: "CallerRunsPolicy"
      thread-name-prefix: "jeepay-"
```

### 3. 使用线程池

在业务代码中注入并使用线程池：

```java
@Service
public class PaymentService {
    
    @Autowired
    private ThreadPoolManager threadPoolManager;
    
    public void processPayment(PaymentOrder order) {
        // 获取支付处理线程池
        ThreadPoolTaskExecutor executor = threadPoolManager.getPaymentProcessPool();
        
        // 异步处理支付
        executor.execute(() -> {
            // 支付处理逻辑
            handlePayment(order);
        });
    }
}
```

## 配置说明

### 配置层级

线程池配置采用三级配置体系，优先级从高到低：

1. **具体线程池配置** - 针对特定线程池的配置
2. **业务类型配置** - 针对业务类型的通用配置
3. **全局默认配置** - 系统全局默认配置

### 全局配置

```yaml
jeepay:
  thread-pool:
    global:
      core-pool-size: 5          # 核心线程数
      max-pool-size: 20          # 最大线程数
      queue-capacity: 100        # 队列容量
      keep-alive-seconds: 300    # 线程存活时间（秒）
      rejected-execution-handler: "CallerRunsPolicy"  # 拒绝策略
      thread-name-prefix: "jeepay-"  # 线程名前缀
      allow-core-thread-time-out: false  # 是否允许核心线程超时
```

### 业务类型配置

```yaml
jeepay:
  thread-pool:
    business-types:
      core-business:
        description: "核心业务线程池"
        core-pool-size: 10
        max-pool-size: 50
        queue-capacity: 200
        rejected-execution-handler: "AbortPolicy"
      
      support-service:
        description: "支撑服务线程池"
        core-pool-size: 3
        max-pool-size: 15
        queue-capacity: 500
        rejected-execution-handler: "DiscardPolicy"
```

### 具体线程池配置

```yaml
jeepay:
  thread-pool:
    pools:
      paymentProcessPool:
        pool-name: "paymentProcessPool"
        description: "支付处理线程池"
        business-type: "core-business"
        core-pool-size: 20
        max-pool-size: 100
        queue-capacity: 200
        keep-alive-seconds: 300
        rejected-execution-handler: "AbortPolicy"
        thread-name-prefix: "payment-"
        monitor-enabled: true
        alarm:
          queue-usage-threshold: 80
          active-thread-threshold: 90
          rejected-task-threshold: 10
          avg-wait-time-threshold: 1000
```

### 拒绝策略说明

| 策略名称 | 描述 | 适用场景 |
|---------|------|----------|
| AbortPolicy | 抛出异常 | 核心业务，需要保证任务执行 |
| CallerRunsPolicy | 调用者执行 | 重要业务，允许同步执行 |
| DiscardPolicy | 直接丢弃 | 非重要业务，可以容忍丢失 |
| DiscardOldestPolicy | 丢弃最老任务 | 实时性要求高的场景 |

## 使用指南

### 预定义线程池

系统提供了以下预定义线程池：

#### 核心业务线程池

```java
// 支付处理线程池
ThreadPoolTaskExecutor paymentPool = threadPoolManager.getPaymentProcessPool();

// 退款处理线程池
ThreadPoolTaskExecutor refundPool = threadPoolManager.getRefundProcessPool();

// 商户通知线程池
ThreadPoolTaskExecutor notifyPool = threadPoolManager.getNotifyMerchantPool();

// 分账处理线程池
ThreadPoolTaskExecutor divisionPool = threadPoolManager.getDivisionProcessPool();
```

#### 支撑服务线程池

```java
// 日志记录线程池
ThreadPoolTaskExecutor logPool = threadPoolManager.getLogRecordPool();

// 缓存更新线程池
ThreadPoolTaskExecutor cachePool = threadPoolManager.getCacheUpdatePool();

// 文件上传线程池
ThreadPoolTaskExecutor filePool = threadPoolManager.getFileUploadPool();
```

#### 定时任务线程池

```java
// 订单补单线程池
ThreadPoolTaskExecutor reissuePool = threadPoolManager.getOrderReissuePool();

// 订单过期处理线程池
ThreadPoolTaskExecutor expiredPool = threadPoolManager.getOrderExpiredPool();

// 对账任务线程池
ThreadPoolTaskExecutor reconPool = threadPoolManager.getReconciliationPool();
```

### 创建自定义线程池

```java
@Service
public class CustomService {
    
    @Autowired
    private ThreadPoolManager threadPoolManager;
    
    public void init() {
        // 创建自定义线程池
        ThreadPoolTaskExecutor customPool = threadPoolManager.createCustomPool(
            "customBusinessPool",  // 线程池名称
            10,                   // 核心线程数
            50,                   // 最大线程数
            200,                  // 队列容量
            "CallerRunsPolicy"    // 拒绝策略
        );
    }
}
```

### 异步方法使用

```java
@Service
public class AsyncService {
    
    @Async("paymentProcessPool")
    public CompletableFuture<String> processPaymentAsync(PaymentOrder order) {
        // 异步处理逻辑
        String result = handlePayment(order);
        return CompletableFuture.completedFuture(result);
    }
}
```

## 监控与告警

### 监控指标

系统自动收集以下监控指标：

- **容量指标**：活跃线程数、队列大小、已完成任务数
- **性能指标**：平均执行时间、任务等待时间
- **健康指标**：拒绝任务数、异常任务数

### 监控接口

```java
@RestController
public class MonitorController {
    
    @Autowired
    private ThreadPoolManager threadPoolManager;
    
    // 获取线程池概览
    @GetMapping("/threadpool/overview")
    public Map<String, Object> getOverview() {
        return threadPoolManager.getPoolOverview();
    }
    
    // 获取所有监控指标
    @GetMapping("/threadpool/metrics")
    public Map<String, ThreadPoolMetrics> getAllMetrics() {
        return threadPoolManager.getAllPoolMetrics();
    }
    
    // 检查健康状态
    @GetMapping("/threadpool/health")
    public Map<String, String> checkHealth() {
        return threadPoolManager.checkPoolHealth();
    }
}
```

### 告警配置

```yaml
jeepay:
  thread-pool:
    pools:
      paymentProcessPool:
        alarm:
          queue-usage-threshold: 80      # 队列使用率告警阈值（%）
          active-thread-threshold: 90    # 活跃线程告警阈值（%）
          rejected-task-threshold: 10    # 任务拒绝告警阈值（次数）
          avg-wait-time-threshold: 1000  # 平均等待时间告警阈值（毫秒）
```

### 告警处理

系统支持以下告警方式：

1. **日志告警** - 记录到应用日志
2. **JMX告警** - 通过JMX监控
3. **自定义告警** - 集成外部告警系统

```java
// 自定义告警处理
@Component
public class CustomAlarmHandler {
    
    @EventListener
    public void handleAlarm(ThreadPoolAlarmEvent event) {
        // 发送钉钉通知
        dingTalkService.sendAlarm(event.getMessage());
        
        // 发送邮件告警
        emailService.sendAlarm(event.getMessage());
    }
}
```

## 最佳实践

### 1. 线程池配置原则

#### 核心线程数配置

```
CPU密集型任务：核心线程数 = CPU核心数 + 1
IO密集型任务：核心线程数 = CPU核心数 * 2
混合型任务：根据实际测试调整
```

#### 队列容量配置

```
高并发场景：较小队列 + 较多线程
稳定负载场景：较大队列 + 适中线程
```

#### 拒绝策略选择

```java
// 核心业务：使用AbortPolicy，确保任务不丢失
@Configuration
public class CoreBusinessConfig {
    
    @Bean
    public ThreadPoolTaskExecutor paymentPool() {
        // 配置为AbortPolicy，支付失败时抛出异常
        return threadPoolManager.getPaymentProcessPool();
    }
}

// 日志记录：使用DiscardPolicy，允许丢弃部分日志
@Configuration  
public class LogConfig {
    
    @Bean
    public ThreadPoolTaskExecutor logPool() {
        // 配置为DiscardPolicy，系统繁忙时可以丢弃部分日志
        return threadPoolManager.getLogRecordPool();
    }
}
```

### 2. 线程池隔离

不同业务使用独立的线程池，避免相互影响：

```java
@Service
public class BusinessService {
    
    @Autowired
    private ThreadPoolManager threadPoolManager;
    
    // 支付业务使用专用线程池
    public void processPayment() {
        threadPoolManager.getPaymentProcessPool().execute(() -> {
            // 支付处理逻辑
        });
    }
    
    // 通知业务使用专用线程池
    public void notifyMerchant() {
        threadPoolManager.getNotifyMerchantPool().execute(() -> {
            // 通知处理逻辑
        });
    }
}
```

### 3. 监控和调优

#### 定期监控

```java
@Component
@Slf4j
public class ThreadPoolMonitor {
    
    @Autowired
    private ThreadPoolManager threadPoolManager;
    
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void monitorThreadPools() {
        Map<String, ThreadPoolMetrics> metrics = threadPoolManager.getAllPoolMetrics();
        
        metrics.forEach((poolName, metric) -> {
            log.info("线程池 {} 监控指标: 活跃线程={}, 队列大小={}, 完成任务={}", 
                poolName, metric.getActiveCount(), 
                metric.getQueueSize(), metric.getCompletedTaskCount());
        });
    }
}
```

#### 动态调整

```java
@Service
public class ThreadPoolTuningService {
    
    @Autowired
    private ThreadPoolManager threadPoolManager;
    
    public void adjustPoolBasedOnLoad() {
        ThreadPoolMetrics metrics = threadPoolManager.getPoolMetrics("paymentProcessPool");
        
        // 根据队列使用率动态调整
        if (metrics.getQueueUsageRate() > 80) {
            threadPoolManager.adjustPoolSize("paymentProcessPool", 
                metrics.getCorePoolSize() + 5, 
                metrics.getMaxPoolSize() + 10);
        }
    }
}
```

### 4. 环境差异化配置

#### 开发环境

```yaml
spring:
  profiles: dev
  
jeepay:
  thread-pool:
    global:
      core-pool-size: 2
      max-pool-size: 5
    pools:
      paymentProcessPool:
        core-pool-size: 3
        max-pool-size: 10
```

#### 生产环境

```yaml
spring:
  profiles: prod
  
jeepay:
  thread-pool:
    global:
      core-pool-size: 10
      max-pool-size: 50
    pools:
      paymentProcessPool:
        core-pool-size: 30
        max-pool-size: 150
```

## 故障排查

### 常见问题

#### 1. 线程池任务拒绝

**现象**：日志中出现 RejectedExecutionException

**原因**：
- 线程池满载
- 队列已满
- 线程池已关闭

**解决方案**：
```java
// 1. 检查线程池配置
ThreadPoolMetrics metrics = threadPoolManager.getPoolMetrics("poolName");
log.info("线程池状态: {}", metrics);

// 2. 调整线程池参数
threadPoolManager.adjustPoolSize("poolName", newCoreSize, newMaxSize);

// 3. 更换拒绝策略
// 配置文件中修改 rejected-execution-handler
```

#### 2. 线程池性能问题

**现象**：任务执行缓慢，等待时间长

**排查步骤**：
```java
// 1. 检查监控指标
Map<String, String> health = threadPoolManager.checkPoolHealth();
health.forEach((pool, status) -> {
    if (!"HEALTHY".equals(status)) {
        log.warn("线程池 {} 状态异常: {}", pool, status);
    }
});

// 2. 分析队列积压
ThreadPoolMetrics metrics = threadPoolManager.getPoolMetrics("poolName");
if (metrics.getQueueUsageRate() > 80) {
    log.warn("队列使用率过高: {}%", metrics.getQueueUsageRate());
}

// 3. 检查线程利用率
if (metrics.getActiveThreadRate() > 95) {
    log.warn("线程利用率过高: {}%", metrics.getActiveThreadRate());
}
```

#### 3. 内存泄漏问题

**现象**：线程数不断增长，内存占用上升

**排查方法**：
```bash
# 查看线程数
jstack <pid> | grep "jeepay-" | wc -l

# 查看线程池状态
jconsole或JVisualVM监控

# 查看堆内存
jmap -histo <pid> | grep ThreadPoolTaskExecutor
```

**解决方案**：
```java
// 确保线程池正确关闭
@PreDestroy
public void destroy() {
    threadPoolManager.destroy();
}
```

## 性能调优

### 1. 线程数调优

#### CPU密集型任务

```yaml
# 适合CPU密集型任务的配置
paymentCalculationPool:
  core-pool-size: 8    # CPU核心数
  max-pool-size: 8     # 等于核心数
  queue-capacity: 100  # 适中的队列
```

#### IO密集型任务

```yaml
# 适合IO密集型任务的配置
dbOperationPool:
  core-pool-size: 16   # CPU核心数 * 2
  max-pool-size: 32    # 核心数 * 2
  queue-capacity: 200  # 较大的队列
```

### 2. 队列调优

#### 高吞吐量场景

```yaml
# 高吞吐量配置
highThroughputPool:
  core-pool-size: 20
  max-pool-size: 20    # 固定线程数
  queue-capacity: 1000 # 大队列
  rejected-execution-handler: "CallerRunsPolicy"
```

#### 低延迟场景

```yaml
# 低延迟配置
lowLatencyPool:
  core-pool-size: 50
  max-pool-size: 100   # 大线程池
  queue-capacity: 10   # 小队列
  rejected-execution-handler: "AbortPolicy"
```

### 3. 监控指标优化

```java
@Component
public class PerformanceOptimizer {
    
    @Autowired
    private ThreadPoolManager threadPoolManager;
    
    @Scheduled(fixedRate = 300000) // 每5分钟执行
    public void optimizePerformance() {
        Map<String, ThreadPoolMetrics> metrics = threadPoolManager.getAllPoolMetrics();
        
        metrics.forEach((poolName, metric) -> {
            // 自动扩容策略
            if (metric.getQueueUsageRate() > 90 && metric.getActiveThreadRate() > 90) {
                int newCoreSize = (int) (metric.getCorePoolSize() * 1.2);
                int newMaxSize = (int) (metric.getMaxPoolSize() * 1.2);
                threadPoolManager.adjustPoolSize(poolName, newCoreSize, newMaxSize);
                log.info("自动扩容线程池 {}: core={}, max={}", poolName, newCoreSize, newMaxSize);
            }
            
            // 自动缩容策略
            if (metric.getQueueUsageRate() < 30 && metric.getActiveThreadRate() < 30) {
                int newCoreSize = Math.max(1, (int) (metric.getCorePoolSize() * 0.8));
                int newMaxSize = Math.max(newCoreSize, (int) (metric.getMaxPoolSize() * 0.8));
                threadPoolManager.adjustPoolSize(poolName, newCoreSize, newMaxSize);
                log.info("自动缩容线程池 {}: core={}, max={}", poolName, newCoreSize, newMaxSize);
            }
        });
    }
}
```

---

## 总结

JeePay线程池配置优化方案提供了：

1. **统一管理** - 集中化的线程池配置和管理
2. **灵活配置** - 多层级配置体系，支持不同环境差异化配置
3. **实时监控** - 完善的监控指标和健康检查
4. **自动告警** - 多级告警机制，及时发现问题
5. **动态调整** - 支持运行时参数调整和自动优化

通过合理使用本框架，可以显著提升系统的性能、稳定性和可维护性。建议在使用过程中结合实际业务场景，持续优化线程池配置，确保系统始终处于最佳状态。