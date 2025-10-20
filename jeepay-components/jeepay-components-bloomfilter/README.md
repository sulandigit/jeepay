# Jeepay 布隆过滤器组件

## 简介

布隆过滤器(Bloom Filter)是一种空间效率很高的概率型数据结构,用于判断一个元素是否在一个集合中。本组件基于Redis的Bitmap实现布隆过滤器功能。

## 特性

- 基于Redis Bitmap实现,支持分布式环境
- 自动计算最优的bit数组大小和哈希函数个数
- 支持单个/批量添加元素
- 支持设置过期时间
- 支持自定义配置参数

## 使用方法

### 1. 添加依赖

在需要使用布隆过滤器的模块的pom.xml中添加依赖:

```xml
<dependency>
    <groupId>com.jeequan</groupId>
    <artifactId>jeepay-components-bloomfilter</artifactId>
</dependency>
```

### 2. 配置参数

在application.yml中添加配置(可选):

```yaml
jeepay:
  bloomfilter:
    enabled: true  # 是否启用布隆过滤器,默认true
    expected-insertions: 10000  # 预期插入数量,默认10000
    false-positive-probability: 0.01  # 错误率,默认0.01(1%)
    key-prefix: "jeepay:bloomfilter:"  # Redis key前缀,默认jeepay:bloomfilter:
```

### 3. 使用示例

#### 基本使用

```java
@Autowired
private BloomFilterService bloomFilterService;

// 添加元素
bloomFilterService.add("user_ids", "user_12345");

// 检查元素是否可能存在
boolean exists = bloomFilterService.mightContain("user_ids", "user_12345");
if (exists) {
    // 元素可能存在,需要进一步查询数据库确认
} else {
    // 元素一定不存在,直接返回
}

// 批量添加元素
bloomFilterService.addAll("user_ids", "user_001", "user_002", "user_003");

// 设置过期时间(1小时)
bloomFilterService.expire("user_ids", 1, TimeUnit.HOURS);

// 删除布隆过滤器
bloomFilterService.delete("user_ids");
```

#### 防止缓存穿透

```java
public User getUserById(String userId) {
    // 先检查布隆过滤器
    if (!bloomFilterService.mightContain("valid_user_ids", userId)) {
        // 布隆过滤器中不存在,说明一定不存在
        return null;
    }
    
    // 查询缓存
    User user = redisUtil.getObject("user:" + userId, User.class);
    if (user != null) {
        return user;
    }
    
    // 查询数据库
    user = userMapper.selectById(userId);
    if (user != null) {
        redisUtil.set("user:" + userId, user, 3600);
    }
    
    return user;
}

// 在系统启动或用户注册时添加到布隆过滤器
@PostConstruct
public void initBloomFilter() {
    List<String> allUserIds = userMapper.selectAllUserIds();
    bloomFilterService.addAll("valid_user_ids", allUserIds.toArray(new String[0]));
}
```

#### 去重场景

```java
// 订单号去重
public boolean isOrderDuplicate(String orderNo) {
    if (bloomFilterService.mightContain("order_nos", orderNo)) {
        // 可能重复,需要查询数据库确认
        return orderMapper.exists(orderNo);
    }
    // 一定不重复
    bloomFilterService.add("order_nos", orderNo);
    return false;
}
```

## 注意事项

1. **误判问题**: 布隆过滤器可能会出现误判(返回元素存在但实际不存在),但不会漏判(返回不存在则一定不存在)
2. **无法删除元素**: 布隆过滤器不支持删除单个元素,只能删除整个过滤器
3. **容量规划**: 需要根据实际业务场景合理设置`expected-insertions`和`false-positive-probability`参数
4. **Redis依赖**: 需要确保Redis服务可用

## 参数说明

- `expected-insertions`: 预期插入的元素数量,影响bit数组大小
- `false-positive-probability`: 误判率,值越小需要的空间越大
- `key-prefix`: Redis中存储的key前缀,建议不同业务使用不同前缀

## 性能优化建议

1. 合理设置预期插入数量,避免过度分配内存
2. 批量添加元素时使用`addAll`方法
3. 根据业务场景设置合适的过期时间
4. 对于高频访问的过滤器,可以考虑使用本地缓存+Redis的组合方案
