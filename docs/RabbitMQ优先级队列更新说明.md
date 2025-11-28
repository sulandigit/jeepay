# RabbitMQ优先级队列功能更新说明

## 更新概述

本次更新为Jeepay项目的RabbitMQ组件添加了优先级队列支持功能。

## 核心修改

### 1. AbstractMQ.java (基类修改)
**文件路径**: `jeepay-components/jeepay-components-mq/src/main/java/com/jeequan/jeepay/components/mq/model/AbstractMQ.java`

**新增方法**:
- `getMaxPriority()`: 获取队列最大优先级，默认返回0（不启用优先级）
- `getMessagePriority()`: 获取当前消息的优先级，默认返回0

### 2. RabbitMQConfig.java (队列配置修改)
**文件路径**: `jeepay-components/jeepay-components-mq/src/main/java/com/jeequan/jeepay/components/mq/vender/rabbitmq/RabbitMQConfig.java`

**修改内容**:
- 队列注册时检查 `getMaxPriority()` 返回值
- 如果 > 0，则创建支持优先级的队列（添加 x-max-priority 参数）
- 如果 = 0，则创建普通队列

### 3. RabbitMQSender.java (消息发送修改)
**文件路径**: `jeepay-components/jeepay-components-mq/src/main/java/com/jeequan/jeepay/components/mq/vender/rabbitmq/RabbitMQSender.java`

**修改内容**:
- `send()` 方法：发送消息时检查优先级，如果设置了优先级则添加到消息属性中
- `send(delay)` 方法：延迟消息也支持优先级设置

## 新增文件

### 4. PriorityPayOrderMQ.java (示例模型)
**文件路径**: `jeepay-components/jeepay-components-mq/src/main/java/com/jeequan/jeepay/components/mq/model/PriorityPayOrderMQ.java`

**功能**:
- 完整的优先级队列使用示例
- 演示如何配置队列最大优先级（返回10）
- 演示如何根据业务类型自动计算消息优先级
- 提供灵活的消息构建方法

### 5. PriorityPayOrderRabbitMQReceiver.java (示例接收器)
**文件路径**: `jeepay-components/jeepay-components-mq/src/main/java/com/jeequan/jeepay/components/mq/vender/rabbitmq/receive/PriorityPayOrderRabbitMQReceiver.java`

**功能**:
- 演示如何接收优先级队列的消息
- 标准的接收器实现模板

### 6. 文档文件
- `docs/RabbitMQ优先级队列使用说明.md` - 详细的使用文档
- `docs/RabbitMQ优先级队列测试用例.md` - 完整的测试示例
- `docs/RabbitMQ优先级队列功能说明.md` - 功能概述和快速开始

## 使用方式

### 创建支持优先级的队列

```java
public class MyMQ extends AbstractMQ {
    
    @Override
    public int getMaxPriority() {
        return 10;  // 启用优先级，范围0-10
    }
    
    @Override
    public int getMessagePriority() {
        return this.priority;  // 返回消息优先级
    }
}
```

### 发送优先级消息

```java
// 高优先级消息
MyMQ highMsg = MyMQ.buildWithPriority(data, 10);
mqSender.send(highMsg);

// 低优先级消息
MyMQ lowMsg = MyMQ.buildWithPriority(data, 1);
mqSender.send(lowMsg);
```

## 兼容性说明

✅ **完全向后兼容**
- 不影响现有MQ功能
- 默认情况下（getMaxPriority()返回0）行为与之前完全一致
- 现有队列继续正常工作

✅ **可选功能**
- 只有在需要时才启用优先级队列
- 通过重写 `getMaxPriority()` 方法控制是否启用

✅ **延迟消息兼容**
- 优先级队列可与延迟消息同时使用
- 延迟消息到期后会按照优先级排序

## 注意事项

⚠️ **队列创建**
- 优先级参数在队列首次创建时设置
- 修改优先级需要删除队列后重新创建

⚠️ **性能影响**
- 推荐使用较小的优先级范围（1-10）
- 优先级范围越大，性能影响越大

⚠️ **适用场景**
- 仅在消息有明确优先级区分时使用
- 消息处理速度很快（无积压）时优先级作用不明显

## 验证方法

1. 启动RabbitMQ: `docker-compose up -d rabbitmq`
2. 启动应用
3. 访问RabbitMQ管理界面: http://localhost:15672
4. 查看队列详情，确认 Features 中有 "Pri" 标识
5. 查看 Arguments 中有 "x-max-priority" 参数

## 相关文档

- [详细使用说明](./RabbitMQ优先级队列使用说明.md)
- [测试用例文档](./RabbitMQ优先级队列测试用例.md)
- [功能概述文档](./RabbitMQ优先级队列功能说明.md)

---

**版本**: v1.0  
**更新日期**: 2025-10-17  
**影响范围**: RabbitMQ组件（可选功能，不影响现有功能）
