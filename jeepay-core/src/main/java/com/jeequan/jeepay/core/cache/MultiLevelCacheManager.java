package com.jeequan.jeepay.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 多级缓存管理器实现
 * L1: Caffeine本地缓存
 * L2: Redis分布式缓存
 *
 * @author jeepay
 * @since 2.3.0
 */
@Slf4j
public class MultiLevelCacheManager implements IMultiLevelCacheManager {

    private final MultiLevelCacheProperties properties;
    private final Cache<String, Object> l1Cache;
    private final RedisTemplate<String, Object> redisTemplate;

    // 统计计数器
    private final AtomicLong l1HitCount = new AtomicLong(0);
    private final AtomicLong l1MissCount = new AtomicLong(0);
    private final AtomicLong l2HitCount = new AtomicLong(0);
    private final AtomicLong l2MissCount = new AtomicLong(0);

    public MultiLevelCacheManager(MultiLevelCacheProperties properties,
                                   RedisTemplate<String, Object> redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;

        // 初始化L1缓存(Caffeine)
        this.l1Cache = buildL1Cache();

        log.info("MultiLevelCacheManager initialized with L1[{}] L2[{}]",
                properties.getL1().getEnabled() ? "enabled" : "disabled",
                properties.getL2().getEnabled() ? "enabled" : "disabled");
    }

    /**
     * 构建L1缓存
     */
    private Cache<String, Object> buildL1Cache() {
        MultiLevelCacheProperties.L1Config l1Config = properties.getL1();
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(l1Config.getMaximumSize())
                .initialCapacity(l1Config.getInitialCapacity())
                .expireAfterWrite(l1Config.getExpireAfterWrite(), TimeUnit.MINUTES)
                .expireAfterAccess(l1Config.getExpireAfterAccess(), TimeUnit.MINUTES);

        if (l1Config.getRecordStats()) {
            builder.recordStats();
        }

        return builder.build();
    }

    @Override
    public <T> T get(String key) {
        return get(key, null);
    }

    @Override
    public <T> T get(String key, Supplier<T> loader) {
        return get(key, loader, properties.getL2().getDefaultTtl(), TimeUnit.SECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> loader, long ttl, TimeUnit unit) {
        String fullKey = buildKey(key);

        // 1. 尝试从L1缓存获取
        if (properties.getL1().getEnabled()) {
            Object value = l1Cache.getIfPresent(fullKey);
            if (value != null) {
                l1HitCount.incrementAndGet();
                log.debug("L1 cache hit for key: {}", fullKey);
                return (T) value;
            }
            l1MissCount.incrementAndGet();
        }

        // 2. 尝试从L2缓存获取
        if (properties.getL2().getEnabled()) {
            Object value = redisTemplate.opsForValue().get(fullKey);
            if (value != null) {
                l2HitCount.incrementAndGet();
                log.debug("L2 cache hit for key: {}", fullKey);
                // 回填L1缓存
                if (properties.getL1().getEnabled()) {
                    l1Cache.put(fullKey, value);
                }
                return (T) value;
            }
            l2MissCount.incrementAndGet();
        }

        // 3. 缓存未命中,执行loader加载数据
        if (loader != null) {
            T value = loader.get();
            if (value != null || properties.getCacheNullValues()) {
                put(fullKey, value, ttl, unit);
            }
            return value;
        }

        return null;
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, properties.getL2().getDefaultTtl(), TimeUnit.SECONDS);
    }

    @Override
    public void put(String key, Object value, long ttl, TimeUnit unit) {
        String fullKey = buildKey(key);

        // 写入L1缓存
        if (properties.getL1().getEnabled()) {
            l1Cache.put(fullKey, value);
        }

        // 写入L2缓存
        if (properties.getL2().getEnabled()) {
            redisTemplate.opsForValue().set(fullKey, value, ttl, unit);
        }

        log.debug("Cache put for key: {}, ttl: {} {}", fullKey, ttl, unit);
    }

    @Override
    public void evict(String key) {
        String fullKey = buildKey(key);

        // 删除L1缓存
        if (properties.getL1().getEnabled()) {
            l1Cache.invalidate(fullKey);
        }

        // 删除L2缓存
        if (properties.getL2().getEnabled()) {
            redisTemplate.delete(fullKey);
        }

        log.debug("Cache evicted for key: {}", fullKey);
    }

    @Override
    public void evict(String... keys) {
        for (String key : keys) {
            evict(key);
        }
    }

    @Override
    public void clear() {
        if (properties.getL1().getEnabled()) {
            l1Cache.invalidateAll();
        }
        // Note: 不清理Redis,避免影响其他节点
        log.info("L1 cache cleared");
    }

    @Override
    public void broadcastEvict(String key) {
        // TODO: 实现MQ广播失效机制
        // 发送失效消息到MQ,通知其他节点删除本地缓存
        evict(key);
        log.debug("Broadcast evict for key: {}", key);
    }

    @Override
    public CacheStats getStats() {
        return new CacheStats(
                l1HitCount.get(),
                l1MissCount.get(),
                l2HitCount.get(),
                l2MissCount.get()
        );
    }

    /**
     * 构建完整的缓存key
     */
    private String buildKey(String key) {
        if (key.startsWith(properties.getKeyPrefix() + ":")) {
            return key;
        }
        return properties.getKeyPrefix() + ":" + key;
    }
}
