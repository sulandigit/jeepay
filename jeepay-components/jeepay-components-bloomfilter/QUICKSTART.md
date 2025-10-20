# 布隆过滤器快速开始指南

## 1. 在你的模块中添加依赖

在需要使用布隆过滤器的模块(如 jeepay-payment、jeepay-manager 等)的 `pom.xml` 中添加:

```xml
<!-- 布隆过滤器组件 -->
<dependency>
    <groupId>com.jeequan</groupId>
    <artifactId>jeepay-components-bloomfilter</artifactId>
</dependency>
```

## 2. 配置参数(可选)

在模块的 `application.yml` 中添加配置:

```yaml
jeepay:
  bloomfilter:
    enabled: true
    expected-insertions: 10000
    false-positive-probability: 0.01
    key-prefix: "jeepay:bloomfilter:"
```

## 3. 使用布隆过滤器

### 方式一: 注入服务使用

```java
import com.jeequan.jeepay.components.bloomfilter.service.BloomFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    @Autowired
    private BloomFilterService bloomFilterService;
    
    // 添加用户ID
    public void addUser(String userId) {
        bloomFilterService.add("valid_users", userId);
    }
    
    // 检查用户是否存在
    public boolean checkUser(String userId) {
        return bloomFilterService.mightContain("valid_users", userId);
    }
}
```

### 方式二: 防止缓存穿透

```java
@Service
public class UserQueryService {
    
    @Autowired
    private BloomFilterService bloomFilterService;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RedisUtil redisUtil;
    
    public User getUserById(String userId) {
        // 1. 先检查布隆过滤器
        if (!bloomFilterService.mightContain("valid_users", userId)) {
            return null; // 一定不存在
        }
        
        // 2. 查询Redis缓存
        User user = redisUtil.getObject("user:" + userId, User.class);
        if (user != null) {
            return user;
        }
        
        // 3. 查询数据库
        user = userMapper.selectById(userId);
        if (user != null) {
            redisUtil.set("user:" + userId, user, 3600);
        }
        
        return user;
    }
}
```

### 方式三: 系统启动时初始化

```java
@Component
public class BloomFilterInitializer {
    
    @Autowired
    private BloomFilterService bloomFilterService;
    
    @Autowired
    private UserMapper userMapper;
    
    @PostConstruct
    public void init() {
        // 从数据库加载所有有效用户ID
        List<String> userIds = userMapper.selectAllUserIds();
        
        // 批量添加到布隆过滤器
        bloomFilterService.addAll("valid_users", 
            userIds.toArray(new String[0]));
        
        // 设置24小时过期
        bloomFilterService.expire("valid_users", 24, TimeUnit.HOURS);
    }
}
```

## 4. 常用API

| 方法 | 说明 |
|------|------|
| `add(filterName, value)` | 添加单个元素 |
| `addAll(filterName, values...)` | 批量添加元素 |
| `mightContain(filterName, value)` | 检查元素是否可能存在 |
| `delete(filterName)` | 删除过滤器 |
| `expire(filterName, timeout, timeUnit)` | 设置过期时间 |

## 5. 使用场景示例

### 场景1: 订单去重
```java
public boolean createOrder(String orderNo) {
    if (bloomFilterService.mightContain("orders", orderNo)) {
        // 可能重复,查数据库确认
        return false;
    }
    bloomFilterService.add("orders", orderNo);
    // 创建订单...
    return true;
}
```

### 场景2: IP黑名单
```java
public boolean isBlocked(String ip) {
    return bloomFilterService.mightContain("ip_blacklist", ip);
}
```

## 6. 注意事项

1. **误判**: 返回true表示"可能存在",需要进一步确认; 返回false表示"一定不存在"
2. **容量**: 超过预期插入数量会增加误判率
3. **删除**: 只能删除整个过滤器,不能删除单个元素
4. **Redis**: 确保Redis服务可用

## 7. 性能优化

- 批量操作使用 `addAll` 而不是多次调用 `add`
- 合理设置过期时间,避免过滤器无限增长
- 根据业务量合理配置 `expected-insertions`

## 更多信息

详见 [README.md](./README.md)
