# Jeepay多级缓存使用指南

## 1. 概述

Jeepay 2.3.0版本引入了多级缓存架构,采用Caffeine(L1本地缓存) + Redis(L2分布式缓存)两级缓存设计,显著提升系统性能并降低Redis负载。

### 1.1 架构优势

- **性能提升**: 热点数据查询性能提升80%以上
- **降低负载**: Redis访问量降低50%以上
- **高可用性**: L1缓存故障不影响L2缓存
- **灵活配置**: 支持针对不同场景灵活配置缓存策略

### 1.2 缓存分层

```
┌─────────────┐
│  应用层请求  │
└──────┬──────┘
       ↓
┌──────────────────┐
│ L1: Caffeine缓存 │  (进程内高速缓存)
│ 命中率目标: 60%+ │
└────────┬─────────┘
         │ 未命中
         ↓
┌──────────────────┐
│ L2: Redis缓存    │  (分布式共享缓存)
│ 命中率目标: 30%+ │
└────────┬─────────┘
         │ 未命中
         ↓
┌──────────────────┐
│   MySQL数据库    │
└──────────────────┘
```

## 2. 快速开始

### 2.1 配置启用

在 `application.yml` 中配置:

```yaml
jeepay:
  cache:
    multi-level:
      enabled: true              # 启用多级缓存
      key-prefix: jeepay         # 缓存key前缀
      cache-null-values: true    # 是否缓存null值
      
      # L1缓存配置
      l1:
        enabled: true
        maximum-size: 5000       # 最大缓存条目数
        expire-after-write: 10   # 写入后过期时间(分钟)
        expire-after-access: 30  # 访问后过期时间(分钟)
        initial-capacity: 500    # 初始容量
        record-stats: true       # 启用统计
      
      # L2缓存配置
      l2:
        enabled: true
        default-ttl: 1800        # 默认TTL(秒)
```

### 2.2 注入使用

```java
@Service
public class YourService {
    
    @Autowired
    private IMultiLevelCacheManager cacheManager;
    
    public MerchantInfo getMerchantInfo(String mchNo) {
        String cacheKey = "mch:info:" + mchNo;
        
        // 从缓存获取,未命中则查数据库
        return cacheManager.get(cacheKey, () -> {
            // 缓存未命中时的数据加载逻辑
            return merchantMapper.selectById(mchNo);
        });
    }
}
```

## 3. API使用示例

### 3.1 基础CRUD操作

#### 查询缓存

```java
// 简单查询
MerchantInfo info = cacheManager.get("mch:info:M001");

// 查询并加载
MerchantInfo info = cacheManager.get("mch:info:M001", () -> {
    return merchantMapper.selectById("M001");
});

// 指定TTL查询
MerchantInfo info = cacheManager.get("mch:info:M001", 
    () -> merchantMapper.selectById("M001"),
    30, TimeUnit.MINUTES);
```

#### 写入缓存

```java
// 使用默认TTL
cacheManager.put("mch:info:M001", merchantInfo);

// 指定TTL
cacheManager.put("mch:info:M001", merchantInfo, 10, TimeUnit.MINUTES);
```

#### 删除缓存

```java
// 删除单个
cacheManager.evict("mch:info:M001");

// 删除多个
cacheManager.evict("mch:info:M001", "mch:info:M002", "mch:info:M003");

// 清空本地缓存
cacheManager.clear();
```

### 3.2 高级场景

#### 商户信息缓存

```java
@Service
public class MerchantCacheService {
    
    @Autowired
    private IMultiLevelCacheManager cacheManager;
    
    @Autowired
    private MerchantMapper merchantMapper;
    
    /**
     * 获取商户信息(带缓存)
     */
    public MerchantInfo getMerchant(String mchNo) {
        String key = "mch:info:" + mchNo;
        return cacheManager.get(key, () -> merchantMapper.selectById(mchNo));
    }
    
    /**
     * 更新商户信息(更新缓存)
     */
    public void updateMerchant(MerchantInfo info) {
        // 更新数据库
        merchantMapper.updateById(info);
        
        // 删除缓存
        String key = "mch:info:" + info.getMchNo();
        cacheManager.broadcastEvict(key);  // 广播失效,通知所有节点
    }
}
```

#### 支付通道配置缓存

```java
@Service
public class PayChannelCacheService {
    
    @Autowired
    private IMultiLevelCacheManager cacheManager;
    
    public PayChannelConfig getChannelConfig(String mchNo, String appId, String wayCode) {
        String key = String.format("pay:channel:%s:%s:%s", mchNo, appId, wayCode);
        
        return cacheManager.get(key, () -> {
            // 复杂查询逻辑
            return payChannelMapper.selectByCondition(mchNo, appId, wayCode);
        }, 5, TimeUnit.MINUTES);  // 支付通道配置5分钟过期
    }
}
```

#### 系统配置缓存

```java
@Service
public class SysConfigCacheService {
    
    @Autowired
    private IMultiLevelCacheManager cacheManager;
    
    public String getConfig(String groupKey, String configKey) {
        String key = String.format("sys:config:%s:%s", groupKey, configKey);
        
        return cacheManager.get(key, () -> {
            SysConfig config = sysConfigMapper.selectByKey(groupKey, configKey);
            return config != null ? config.getConfigValue() : null;
        }, 30, TimeUnit.MINUTES);  // 系统配置30分钟过期
    }
    
    /**
     * 配置变更时清理缓存
     */
    @EventListener
    public void onConfigChanged(ConfigChangedEvent event) {
        String key = String.format("sys:config:%s:%s", 
            event.getGroupKey(), event.getConfigKey());
        cacheManager.broadcastEvict(key);
    }
}
```

## 4. 缓存策略配置

### 4.1 推荐配置

| 数据类型 | L1最大条目 | L1写入过期 | L1访问过期 | L2 TTL | 更新策略 |
|---------|-----------|-----------|-----------|--------|----------|
| 系统配置 | 1000 | 30分钟 | 60分钟 | 1小时 | 广播失效 |
| 商户信息 | 3000 | 10分钟 | 30分钟 | 30分钟 | 主动删除 |
| 支付通道 | 5000 | 5分钟 | 15分钟 | 10分钟 | 主动删除 |
| 权限信息 | 2000 | 15分钟 | 30分钟 | 30分钟 | 广播失效 |
| 订单临时数据 | 0(不缓存) | - | - | 30分钟 | 过期删除 |

### 4.2 不同服务的配置

#### jeepay-manager配置

```yaml
jeepay:
  cache:
    multi-level:
      l1:
        maximum-size: 5000
        expire-after-write: 10
        expire-after-access: 30
```

#### jeepay-merchant配置

```yaml
jeepay:
  cache:
    multi-level:
      l1:
        maximum-size: 3000
        expire-after-write: 10
        expire-after-access: 30
```

#### jeepay-payment配置

```yaml
jeepay:
  cache:
    multi-level:
      l1:
        maximum-size: 10000      # 支付网关需要更大容量
        expire-after-write: 5     # 更短的过期时间
        expire-after-access: 15
```

## 5. 缓存一致性保证

### 5.1 更新模式

#### Cache-Aside模式(推荐)

```java
// 写操作
public void updateData(String id, Object data) {
    // 1. 更新数据库
    mapper.updateById(id, data);
    
    // 2. 删除缓存
    cacheManager.evict("data:" + id);
}

// 读操作
public Object getData(String id) {
    return cacheManager.get("data:" + id, () -> {
        return mapper.selectById(id);
    });
}
```

#### Write-Through模式

```java
public void updateData(String id, Object data) {
    // 1. 更新数据库
    mapper.updateById(id, data);
    
    // 2. 更新缓存
    cacheManager.put("data:" + id, data);
}
```

### 5.2 广播失效机制

```java
// 更新数据时广播失效消息
cacheManager.broadcastEvict("mch:info:M001");

// 集群中其他节点收到消息后删除本地缓存
// 下次查询时重新从L2或DB加载
```

## 6. 监控与统计

### 6.1 获取缓存统计

```java
IMultiLevelCacheManager.CacheStats stats = cacheManager.getStats();

System.out.println(stats.toString());
// 输出: CacheStats{L1: 1200/1500, L2: 250/300, Total: 1450/1800 (80.56% hit rate)}
```

### 6.2 监控指标

```java
@Component
public class CacheMonitor {
    
    @Autowired
    private IMultiLevelCacheManager cacheManager;
    
    @Scheduled(fixedRate = 60000)  // 每分钟统计一次
    public void reportCacheStats() {
        CacheStats stats = cacheManager.getStats();
        
        log.info("=== Cache Statistics ===");
        log.info("L1 Hit: {}, Miss: {}", stats.getL1HitCount(), stats.getL1MissCount());
        log.info("L2 Hit: {}, Miss: {}", stats.getL2HitCount(), stats.getL2MissCount());
        log.info("Total Hit Rate: {:.2f}%", stats.getTotalHitRate() * 100);
    }
}
```

## 7. 最佳实践

### 7.1 缓存Key设计

```java
// 推荐格式: {业务模块}:{数据类型}:{唯一标识}
"mch:info:M001"              // 商户信息
"pay:channel:M001:A001:wxpay" // 支付通道配置
"sys:config:system:timeout"   // 系统配置
"user:perm:U001"              // 用户权限
```

### 7.2 避免缓存穿透

```java
// 方式1: 缓存null值
jeepay.cache.multi-level.cache-null-values=true

// 方式2: 布隆过滤器
@Autowired
private BloomFilter<String> bloomFilter;

public Object getData(String id) {
    if (!bloomFilter.mightContain(id)) {
        return null;  // 数据不存在
    }
    return cacheManager.get("data:" + id, () -> mapper.selectById(id));
}
```

### 7.3 避免缓存雪崩

```java
// 设置随机TTL
long ttl = 1800 + ThreadLocalRandom.current().nextInt(300);
cacheManager.put(key, value, ttl, TimeUnit.SECONDS);
```

### 7.4 避免缓存击穿

```java
// MultiLevelCacheManager内部已实现分布式锁机制
// 同一key的并发请求,只有一个会查询DB,其他等待
```

## 8. 性能优化建议

1. **合理设置容量**: 根据实际数据量设置L1最大容量,避免频繁淘汰
2. **调整过期时间**: 根据数据更新频率调整TTL
3. **监控命中率**: 保持L1命中率>60%, 总命中率>85%
4. **避免大value**: 单个缓存值建议<1MB
5. **批量操作**: 尽量使用批量操作减少网络开销

## 9. 故障排查

### 9.1 缓存未生效

检查项:
- 配置是否正确
- Bean是否正确注入
- Redis连接是否正常

### 9.2 命中率过低

分析原因:
- L1容量过小导致频繁淘汰
- TTL设置过短
- 缓存key设计不合理

### 9.3 内存占用过高

优化措施:
- 减小L1最大容量
- 缩短过期时间
- 只缓存必要数据

## 10. 参考资料

- [Caffeine文档](https://github.com/ben-manes/caffeine)
- [Spring Data Redis文档](https://spring.io/projects/spring-data-redis)
