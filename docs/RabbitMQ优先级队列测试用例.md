# RabbitMQ优先级队列测试用例

## 测试场景说明

本测试用例演示如何使用RabbitMQ优先级队列处理不同优先级的支付订单消息。

## 一、环境准备

### 1.1 配置RabbitMQ

在 `application.yml` 中确保RabbitMQ配置正确：

```yaml
isys:
  mq:
    vender: rabbitMQ

spring:
  rabbitmq:
    addresses: 127.0.0.1:5672
    username: admin
    password: admin
    virtual-host: jeepay
```

### 1.2 启动RabbitMQ

```bash
# 使用Docker启动RabbitMQ
docker-compose up -d rabbitmq

# 访问管理界面
http://localhost:15672
用户名: admin
密码: admin
```

## 二、消息发送测试

### 2.1 创建测试服务类

```java
package com.jeequan.jeepay.service.test;

import com.jeequan.jeepay.components.mq.model.PriorityPayOrderMQ;
import com.jeequan.jeepay.components.mq.vender.IMQSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PriorityQueueTestService {

    @Autowired
    private IMQSender mqSender;

    /**
     * 测试发送不同优先级的消息
     */
    public void testSendPriorityMessages() {
        
        // 1. 发送10个普通订单 (优先级1)
        log.info("========== 发送普通订单 ==========");
        for (int i = 1; i <= 10; i++) {
            PriorityPayOrderMQ msg = PriorityPayOrderMQ.build(
                "NORMAL_ORDER_" + i,
                10000L,
                1  // 普通订单
            );
            mqSender.send(msg);
            log.info("发送普通订单: {}, 优先级: {}", "NORMAL_ORDER_" + i, msg.getMessagePriority());
        }

        // 2. 发送5个VIP订单 (优先级5)
        log.info("========== 发送VIP订单 ==========");
        for (int i = 1; i <= 5; i++) {
            PriorityPayOrderMQ msg = PriorityPayOrderMQ.build(
                "VIP_ORDER_" + i,
                50000L,
                2  // VIP订单
            );
            mqSender.send(msg);
            log.info("发送VIP订单: {}, 优先级: {}", "VIP_ORDER_" + i, msg.getMessagePriority());
        }

        // 3. 发送3个紧急订单 (优先级10)
        log.info("========== 发送紧急订单 ==========");
        for (int i = 1; i <= 3; i++) {
            PriorityPayOrderMQ msg = PriorityPayOrderMQ.build(
                "URGENT_ORDER_" + i,
                100000L,
                3  // 紧急订单
            );
            mqSender.send(msg);
            log.info("发送紧急订单: {}, 优先级: {}", "URGENT_ORDER_" + i, msg.getMessagePriority());
        }

        log.info("========== 所有消息发送完成 ==========");
        log.info("预期消费顺序: 紧急订单(优先级10) -> VIP订单(优先级5) -> 普通订单(优先级1)");
    }

    /**
     * 测试自定义优先级
     */
    public void testCustomPriority() {
        
        log.info("========== 测试自定义优先级 ==========");
        
        // 发送不同优先级的消息
        for (int priority = 0; priority <= 10; priority++) {
            PriorityPayOrderMQ msg = PriorityPayOrderMQ.buildWithPriority(
                "CUSTOM_ORDER_" + priority,
                10000L,
                1,
                priority  // 自定义优先级 0-10
            );
            mqSender.send(msg);
            log.info("发送订单: {}, 自定义优先级: {}", "CUSTOM_ORDER_" + priority, priority);
        }
    }

    /**
     * 测试延迟消息 + 优先级
     */
    public void testDelayWithPriority() {
        
        log.info("========== 测试延迟消息 + 优先级 ==========");
        
        // 发送3秒后到期的高优先级消息
        PriorityPayOrderMQ highPriorityMsg = PriorityPayOrderMQ.buildWithPriority(
            "DELAY_HIGH_PRIORITY",
            100000L,
            3,
            10
        );
        mqSender.send(highPriorityMsg, 3);  // 延迟3秒
        log.info("发送延迟消息(3秒): 高优先级订单");

        // 发送5秒后到期的低优先级消息
        PriorityPayOrderMQ lowPriorityMsg = PriorityPayOrderMQ.buildWithPriority(
            "DELAY_LOW_PRIORITY",
            10000L,
            1,
            1
        );
        mqSender.send(lowPriorityMsg, 5);  // 延迟5秒
        log.info("发送延迟消息(5秒): 低优先级订单");
    }
}
```

### 2.2 创建测试控制器

```java
package com.jeequan.jeepay.controller.test;

import com.jeequan.jeepay.service.test.PriorityQueueTestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Api(tags = "RabbitMQ优先级队列测试")
@RestController
@RequestMapping("/api/test/priority-queue")
public class PriorityQueueTestController {

    @Autowired
    private PriorityQueueTestService testService;

    @ApiOperation("测试发送不同优先级的消息")
    @GetMapping("/send-priority-messages")
    public String testSendPriorityMessages() {
        testService.testSendPriorityMessages();
        return "消息发送成功，请查看日志和RabbitMQ管理界面";
    }

    @ApiOperation("测试自定义优先级")
    @GetMapping("/custom-priority")
    public String testCustomPriority() {
        testService.testCustomPriority();
        return "自定义优先级消息发送成功";
    }

    @ApiOperation("测试延迟消息 + 优先级")
    @GetMapping("/delay-with-priority")
    public String testDelayWithPriority() {
        testService.testDelayWithPriority();
        return "延迟消息发送成功";
    }
}
```

## 三、消息接收测试

### 3.1 创建消息接收器实现

```java
package com.jeequan.jeepay.service.impl;

import com.jeequan.jeepay.components.mq.model.PriorityPayOrderMQ;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PriorityPayOrderMQReceiverImpl implements PriorityPayOrderMQ.IMQReceiver {

    @Override
    public void receive(PriorityPayOrderMQ.MsgPayload payload) {
        
        log.info("========== 接收到优先级订单消息 ==========");
        log.info("订单号: {}", payload.getPayOrderId());
        log.info("订单金额: {}", payload.getAmount());
        log.info("业务类型: {}", getBizTypeName(payload.getBizType()));
        log.info("==========================================");
        
        // 模拟业务处理
        try {
            processOrder(payload);
        } catch (Exception e) {
            log.error("处理订单失败: {}", payload.getPayOrderId(), e);
        }
    }

    /**
     * 处理订单业务逻辑
     */
    private void processOrder(PriorityPayOrderMQ.MsgPayload payload) {
        // 这里实现具体的业务逻辑
        // 例如：更新订单状态、发送通知、记录日志等
        
        // 模拟处理时间
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log.info("订单 {} 处理完成", payload.getPayOrderId());
    }

    /**
     * 获取业务类型名称
     */
    private String getBizTypeName(Integer bizType) {
        if (bizType == null) {
            return "未知";
        }
        switch (bizType) {
            case 1:
                return "普通订单";
            case 2:
                return "VIP订单";
            case 3:
                return "紧急订单";
            default:
                return "未知类型";
        }
    }
}
```

## 四、测试步骤

### 4.1 基础功能测试

1. **启动应用**
```bash
# 启动RabbitMQ
docker-compose up -d rabbitmq

# 启动应用服务
mvn spring-boot:run
```

2. **发送测试消息**
```bash
# 访问测试接口
curl http://localhost:9216/api/test/priority-queue/send-priority-messages
```

3. **观察消费顺序**
   - 查看应用日志
   - 预期顺序：紧急订单 -> VIP订单 -> 普通订单

### 4.2 RabbitMQ管理界面验证

1. 访问 http://localhost:15672
2. 进入 Queues 标签页
3. 找到 `QUEUE_PRIORITY_PAY_ORDER` 队列
4. 查看队列特性：
   - Features: Pri (表示启用了优先级)
   - Arguments: x-max-priority=10

### 4.3 性能测试

```java
/**
 * 批量发送测试 - 验证优先级队列性能
 */
public void performanceTest() {
    long startTime = System.currentTimeMillis();
    
    // 发送1000条不同优先级的消息
    for (int i = 0; i < 1000; i++) {
        int priority = i % 10;  // 0-9循环
        PriorityPayOrderMQ msg = PriorityPayOrderMQ.buildWithPriority(
            "PERF_TEST_" + i,
            10000L,
            1,
            priority
        );
        mqSender.send(msg);
    }
    
    long endTime = System.currentTimeMillis();
    log.info("发送1000条消息耗时: {} ms", (endTime - startTime));
}
```

## 五、预期结果

### 5.1 消息发送日志

```
========== 发送普通订单 ==========
发送普通订单: NORMAL_ORDER_1, 优先级: 1
发送普通订单: NORMAL_ORDER_2, 优先级: 1
...
========== 发送VIP订单 ==========
发送VIP订单: VIP_ORDER_1, 优先级: 5
发送VIP订单: VIP_ORDER_2, 优先级: 5
...
========== 发送紧急订单 ==========
发送紧急订单: URGENT_ORDER_1, 优先级: 10
发送紧急订单: URGENT_ORDER_2, 优先级: 10
...
```

### 5.2 消息消费日志

```
========== 接收到优先级订单消息 ==========
订单号: URGENT_ORDER_1
订单金额: 100000
业务类型: 紧急订单
==========================================
订单 URGENT_ORDER_1 处理完成

========== 接收到优先级订单消息 ==========
订单号: URGENT_ORDER_2
订单金额: 100000
业务类型: 紧急订单
==========================================
订单 URGENT_ORDER_2 处理完成

... (继续消费VIP订单和普通订单)
```

## 六、常见问题排查

### 6.1 消息没有按优先级消费

**检查项**：
1. 队列是否正确配置了优先级参数
2. 队列中是否有消息积压（优先级在有积压时才体现）
3. 消息是否正确设置了优先级

**解决方法**：
```bash
# 查看队列详情
rabbitmqadmin get queue=QUEUE_PRIORITY_PAY_ORDER

# 删除队列重新创建
rabbitmqadmin delete queue name=QUEUE_PRIORITY_PAY_ORDER
# 重启应用，队列会自动重新创建
```

### 6.2 队列未创建或未启用优先级

**解决方法**：
1. 确保 `PriorityPayOrderMQ.getMaxPriority()` 返回值 > 0
2. 重启应用让队列重新创建
3. 检查RabbitMQ日志确认队列创建成功

### 6.3 消息发送后无法消费

**检查项**：
1. 确认已实现 `PriorityPayOrderMQ.IMQReceiver` 接口
2. 确认接收器类上有 `@Component` 注解
3. 确认配置了正确的 vender: rabbitMQ

## 七、测试清单

- [ ] 基础功能测试：发送和接收优先级消息
- [ ] 优先级顺序验证：高优先级消息先被消费
- [ ] 自定义优先级测试：手动指定消息优先级
- [ ] 延迟消息 + 优先级：两种特性结合使用
- [ ] 性能测试：批量发送消息的性能表现
- [ ] 异常处理测试：消息处理失败的处理机制
- [ ] 管理界面验证：RabbitMQ界面确认队列配置
- [ ] 并发测试：多消费者场景下的优先级表现

## 八、测试数据参考

| 测试场景 | 消息数量 | 优先级分布 | 预期结果 |
|---------|---------|----------|---------|
| 基础测试 | 18 | 10(3), 5(5), 1(10) | 按优先级消费 |
| 自定义测试 | 11 | 0-10各1条 | 从10到0顺序消费 |
| 性能测试 | 1000 | 0-9循环 | 消费时高优先级优先 |
| 延迟测试 | 2 | 10(延迟3s), 1(延迟5s) | 延迟后按优先级消费 |

---

**测试环境**: RabbitMQ 3.x + Spring Boot 2.x  
**更新时间**: 2025-10-17
