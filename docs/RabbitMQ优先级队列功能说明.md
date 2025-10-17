# RabbitMQ优先级队列功能实现

## 概述

本次更新为Jeepay项目的RabbitMQ组件添加了优先级队列支持，允许消息根据优先级进行排序和处理。优先级高的消息会被优先消费，适用于需要区分消息重要程度的业务场景。

## 功能特性

✅ **队列优先级配置** - 支持在队列创建时自动配置优先级参数  
✅ **消息优先级设置** - 支持在发送消息时指定消息优先级  
✅ **延迟消息兼容** - 优先级队列与延迟消息功能可同时使用  
✅ **向后兼容** - 不影响现有MQ功能，原有队列继续正常工作  
✅ **完整示例** - 提供完整的示例代码和使用文档  

## 修改文件列表

### 核心代码修改

1. **AbstractMQ.java** - MQ基类
   - 新增 `getMaxPriority()` 方法：获取队列最大优先级
   - 新增 `getMessagePriority()` 方法：获取消息优先级
   - 路径：`jeepay-components/jeepay-components-mq/src/main/java/com/jeequan/jeepay/components/mq/model/`

2. **RabbitMQConfig.java** - RabbitMQ配置类
   - 修改队列创建逻辑，支持优先级参数配置
   - 根据 `getMaxPriority()` 返回值自动创建支持优先级的队列
   - 路径：`jeepay-components/jeepay-components-mq/src/main/java/com/jeequan/jeepay/components/mq/vender/rabbitmq/`

3. **RabbitMQSender.java** - RabbitMQ消息发送器
   - 修改 `send()` 方法，支持发送带优先级的消息
   - 修改 `send(delay)` 方法，支持延迟消息 + 优先级
   - 路径：`jeepay-components/jeepay-components-mq/src/main/java/com/jeequan/jeepay/components/mq/vender/rabbitmq/`

### 示例代码

4. **PriorityPayOrderMQ.java** - 优先级队列示例模型
   - 完整的优先级队列使用示例
   - 演示如何配置队列优先级和消息优先级
   - 提供根据业务类型自动计算优先级的方法
   - 路径：`jeepay-components/jeepay-components-mq/src/main/java/com/jeequan/jeepay/components/mq/model/`

5. **PriorityPayOrderRabbitMQReceiver.java** - 优先级队列接收器示例
   - 演示如何接收优先级队列的消息
   - 路径：`jeepay-components/jeepay-components-mq/src/main/java/com/jeequan/jeepay/components/mq/vender/rabbitmq/receive/`

### 文档

6. **RabbitMQ优先级队列使用说明.md** - 详细使用文档
   - 功能介绍和使用方法
   - 配置说明和最佳实践
   - 常见问题和解决方案
   - 路径：`docs/`

7. **RabbitMQ优先级队列测试用例.md** - 测试用例文档
   - 完整的测试代码示例
   - 测试步骤和预期结果
   - 问题排查指南
   - 路径：`docs/`

## 快速开始

### 1. 创建支持优先级的MQ模型

```java
public class MyPriorityMQ extends AbstractMQ {
    
    @Override
    public int getMaxPriority() {
        return 10;  // 设置队列最大优先级为10
    }
    
    @Override
    public int getMessagePriority() {
        return this.priority;  // 返回当前消息的优先级
    }
}
```

### 2. 发送优先级消息

```java
@Autowired
private IMQSender mqSender;

// 发送高优先级消息
MyPriorityMQ highMsg = MyPriorityMQ.buildWithPriority(data, 10);
mqSender.send(highMsg);

// 发送低优先级消息
MyPriorityMQ lowMsg = MyPriorityMQ.buildWithPriority(data, 1);
mqSender.send(lowMsg);
```

### 3. 接收优先级消息

```java
@Component
@ConditionalOnProperty(name = MQVenderCS.YML_VENDER_KEY, havingValue = MQVenderCS.RABBIT_MQ)
@ConditionalOnBean(MyPriorityMQ.IMQReceiver.class)
public class MyPriorityMQReceiver implements IMQMsgReceiver {
    
    @Autowired
    private MyPriorityMQ.IMQReceiver mqReceiver;
    
    @Override
    @RabbitListener(queues = MyPriorityMQ.MQ_NAME)
    public void receiveMsg(String msg){
        mqReceiver.receive(MyPriorityMQ.parse(msg));
    }
}
```

## 使用建议

### 优先级范围

- **推荐范围**: 1-10
- **支持范围**: 1-255  
- **性能考虑**: 优先级范围越大，对RabbitMQ性能影响越大

### 适用场景

✅ **适合使用优先级队列**：
- 订单处理（VIP订单优先）
- 支付通知（大额支付优先）
- 告警消息（紧急告警优先）
- 任务调度（重要任务优先）

❌ **不适合使用优先级队列**：
- 消息处理速度很快（无积压）
- 所有消息同等重要
- 对性能要求极高的场景

### 注意事项

⚠️ **队列创建**：
- 优先级必须在队列首次创建时声明
- 修改优先级需要删除队列后重新创建

⚠️ **性能影响**：
- 优先级队列在有消息积压时才能体现优势
- 优先级范围越大，性能影响越大

⚠️ **广播模式**：
- BROADCAST模式不支持优先级队列
- 仅QUEUE模式支持优先级功能

## 技术细节

### 实现原理

1. **队列声明**：在 `RabbitMQConfig` 中创建队列时，如果 `getMaxPriority() > 0`，则添加 `x-max-priority` 参数
2. **消息发送**：在 `RabbitMQSender` 中发送消息时，如果 `getMessagePriority() > 0`，则设置消息的 `priority` 属性
3. **消息排序**：RabbitMQ根据消息的优先级属性进行排序，优先级高的消息先被消费

### 兼容性

- ✅ 向后兼容：不影响现有MQ功能
- ✅ 可选功能：通过 `getMaxPriority()` 返回0可禁用优先级
- ✅ 延迟消息：优先级可与延迟消息同时使用
- ✅ 多消费者：支持多消费者竞争消费

## 示例代码

详细示例请参考：
- `PriorityPayOrderMQ.java` - 完整的MQ模型示例
- `PriorityPayOrderRabbitMQReceiver.java` - 消息接收器示例
- `docs/RabbitMQ优先级队列测试用例.md` - 测试代码示例

## 文档链接

📖 [RabbitMQ优先级队列使用说明](./docs/RabbitMQ优先级队列使用说明.md)  
📖 [RabbitMQ优先级队列测试用例](./docs/RabbitMQ优先级队列测试用例.md)  
📖 [RabbitMQ官方文档](https://www.rabbitmq.com/priority.html)

## 测试验证

运行测试验证优先级队列功能：

```bash
# 1. 启动RabbitMQ
docker-compose up -d rabbitmq

# 2. 启动应用
mvn spring-boot:run

# 3. 访问测试接口
curl http://localhost:9216/api/test/priority-queue/send-priority-messages

# 4. 查看RabbitMQ管理界面
http://localhost:15672
```

## 版本信息

- **实现版本**: v1.0
- **更新时间**: 2025-10-17
- **兼容性**: RabbitMQ 3.x+, Spring Boot 2.x+

## 贡献者

- 优先级队列功能实现和文档编写

## 许可证

本项目遵循 GNU LESSER GENERAL PUBLIC LICENSE 3.0 许可证
