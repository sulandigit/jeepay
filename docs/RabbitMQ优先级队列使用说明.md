# RabbitMQ优先级队列使用说明

## 一、概述

RabbitMQ优先级队列允许消息根据优先级进行排序和处理。优先级高的消息会被优先消费，适用于需要区分消息重要程度的业务场景。

## 二、实现原理

本项目已在MQ组件中集成了RabbitMQ优先级队列支持，主要涉及以下几个核心类：

1. **AbstractMQ** - 添加了优先级支持方法
2. **RabbitMQConfig** - 支持创建带优先级参数的队列
3. **RabbitMQSender** - 支持发送带优先级的消息
4. **PriorityPayOrderMQ** - 优先级队列使用示例

## 三、使用方法

### 3.1 创建支持优先级的MQ模型

继承`AbstractMQ`类并重写以下两个方法：

```java
@Override
public int getMaxPriority() {
    return 10;  // 设置队列支持的最大优先级(推荐1-10)
}

@Override
public int getMessagePriority() {
    return this.messagePriority;  // 返回当前消息的优先级
}
```

### 3.2 完整示例

参考 `PriorityPayOrderMQ.java` 文件，这是一个完整的优先级队列实现示例：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriorityPayOrderMQ extends AbstractMQ {

    public static final String MQ_NAME = "QUEUE_PRIORITY_PAY_ORDER";
    
    private MsgPayload payload;
    private int messagePriority;  // 消息优先级

    @Override
    public int getMaxPriority() {
        return 10;  // 队列最大优先级
    }

    @Override
    public int getMessagePriority() {
        return this.messagePriority;  // 当前消息优先级
    }

    // 构建不同优先级的消息
    public static PriorityPayOrderMQ build(String orderId, Long amount, Integer bizType) {
        int priority = calculatePriority(bizType);
        return new PriorityPayOrderMQ(new MsgPayload(orderId, amount, bizType), priority);
    }
}
```

### 3.3 发送优先级消息

使用MQ发送器发送消息：

```java
@Autowired
private IMQSender mqSender;

// 发送普通订单 - 优先级1
PriorityPayOrderMQ normalOrder = PriorityPayOrderMQ.build("ORDER001", 10000L, 1);
mqSender.send(normalOrder);

// 发送VIP订单 - 优先级5
PriorityPayOrderMQ vipOrder = PriorityPayOrderMQ.build("ORDER002", 50000L, 2);
mqSender.send(vipOrder);

// 发送紧急订单 - 优先级10
PriorityPayOrderMQ urgentOrder = PriorityPayOrderMQ.build("ORDER003", 100000L, 3);
mqSender.send(urgentOrder);
```

### 3.4 接收优先级消息

创建接收器实现消息消费：

```java
@Component
@ConditionalOnProperty(name = MQVenderCS.YML_VENDER_KEY, havingValue = MQVenderCS.RABBIT_MQ)
@ConditionalOnBean(PriorityPayOrderMQ.IMQReceiver.class)
public class PriorityPayOrderRabbitMQReceiver implements IMQMsgReceiver {

    @Autowired
    private PriorityPayOrderMQ.IMQReceiver mqReceiver;

    @Override
    @RabbitListener(queues = PriorityPayOrderMQ.MQ_NAME)
    public void receiveMsg(String msg){
        mqReceiver.receive(PriorityPayOrderMQ.parse(msg));
    }
}
```

## 四、优先级配置建议

### 4.1 优先级范围

- **推荐范围**: 1-10
- **支持范围**: 1-255
- **默认值**: 0 (不启用优先级)

### 4.2 性能影响

1. **优先级范围越大，性能影响越大**
   - 优先级值越大，RabbitMQ需要维护的索引越多
   - 推荐使用较小的优先级范围以获得更好的性能

2. **消息堆积情况**
   - 优先级队列在消息堆积时才能体现优势
   - 如果消息处理速度很快，优先级作用不明显

### 4.3 业务场景示例

| 业务类型 | 优先级 | 说明 |
|---------|-------|------|
| 紧急订单/退款 | 10 | 最高优先级，立即处理 |
| VIP用户订单 | 5-7 | 高优先级，优先处理 |
| 普通订单 | 1-3 | 正常优先级 |
| 批量任务 | 0 | 最低优先级，空闲时处理 |

## 五、注意事项

### 5.1 队列声明

- 优先级必须在队列**首次创建时**声明
- 如果队列已存在且未启用优先级，需要**删除队列后重新创建**
- 修改`getMaxPriority()`后需要重启应用

### 5.2 消息优先级

- 消息优先级必须在 **0 到 getMaxPriority()** 范围内
- 超出范围的优先级会被忽略
- 优先级为0的消息优先级最低

### 5.3 延迟消息

- 优先级队列与延迟消息**可以同时使用**
- 延迟消息到期后会按照优先级进行排序

### 5.4 广播模式

- **BROADCAST模式不支持优先级队列**
- 仅QUEUE模式支持优先级功能

## 六、RabbitMQ配置

### 6.1 启用RabbitMQ

在 `application.yml` 中配置：

```yaml
isys:
  mq:
    vender: rabbitMQ  # 使用RabbitMQ作为MQ厂商

spring:
  rabbitmq:
    addresses: 127.0.0.1:5672
    username: admin
    password: admin
    virtual-host: jeepay
```

### 6.2 Docker部署

参考 `docker-compose.yml` 中的RabbitMQ配置：

```yaml
rabbitmq:
  build:
    context: ./docker/rabbitmq
    dockerfile: Dockerfile
  hostname: rabbitmq
  container_name: jeepay-rabbitmq
  image: jeepay-rabbitmq:latest
  ports:
    - "15672:15672"  # 管理界面
    - "5672:5672"    # AMQP端口
  environment:
    RABBITMQ_DEFAULT_USER: 'admin'
    RABBITMQ_DEFAULT_PASS: 'admin'
    RABBITMQ_DEFAULT_VHOST: 'jeepay'
```

## 七、验证优先级队列

### 7.1 RabbitMQ管理界面

访问 `http://localhost:15672`，在队列详情中可以看到：

```
Features: Pri (表示启用了优先级)
Arguments: x-max-priority=10
```

### 7.2 测试代码

```java
// 批量发送不同优先级的消息
for (int i = 0; i < 100; i++) {
    int priority = i % 3;  // 0, 1, 2 三种优先级
    PriorityPayOrderMQ msg = PriorityPayOrderMQ.buildWithPriority(
        "ORDER" + i, 
        10000L, 
        1, 
        priority
    );
    mqSender.send(msg);
}
// 观察消费顺序，高优先级消息会被优先消费
```

## 八、最佳实践

### 8.1 优先级设计原则

1. **明确业务优先级**：根据实际业务需求定义优先级等级
2. **控制优先级范围**：避免使用过大的优先级范围
3. **合理分配优先级**：不要所有消息都使用最高优先级
4. **动态调整机制**：可以根据业务类型动态计算优先级

### 8.2 监控与调优

1. **监控队列积压情况**：优先级队列在有积压时才有意义
2. **观察消费速度**：确保优先级策略符合预期
3. **定期review优先级策略**：根据业务变化调整优先级分配

### 8.3 异常处理

```java
// 优先级消息处理失败时的处理策略
@Override
public void receive(MsgPayload payload) {
    try {
        // 处理业务逻辑
        processOrder(payload);
    } catch (Exception e) {
        // 失败后可以选择：
        // 1. 重新发送（可降低优先级）
        // 2. 记录到失败队列
        // 3. 人工介入处理
        handleException(payload, e);
    }
}
```

## 九、常见问题

### Q1: 为什么消息没有按优先级消费？

**A**: 可能原因：
1. 队列中没有消息积压（消息处理速度很快）
2. 队列未正确配置优先级参数
3. 消息优先级未正确设置

### Q2: 修改优先级后不生效？

**A**: 需要：
1. 删除现有队列
2. 重启应用，重新创建队列
3. 或者使用不同的队列名称

### Q3: 优先级队列对性能影响有多大？

**A**: 
- 优先级范围1-10：性能影响较小，可忽略
- 优先级范围10-100：有一定性能影响
- 优先级范围>100：性能影响较大，不推荐

### Q4: 是否所有消息都需要设置优先级？

**A**: 
- 不需要，只对需要区分优先级的业务使用
- 普通消息可以继续使用原有的MQ模型
- 优先级队列和普通队列可以共存

## 十、相关资源

- [RabbitMQ官方文档 - 优先级队列](https://www.rabbitmq.com/priority.html)
- 项目MQ组件路径: `jeepay-components/jeepay-components-mq`
- 示例代码: `PriorityPayOrderMQ.java`
- RabbitMQ管理界面: http://localhost:15672

---

**更新时间**: 2025-10-17  
**版本**: v1.0
