package com.jeequan.jeepay.core.cache;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 多级缓存管理器接口
 *
 * @author jeepay
 * @since 2.3.0
 */
public interface IMultiLevelCacheManager {

    /**
     * 获取缓存值
     *
     * @param key 缓存key
     * @return 缓存值,不存在返回null
     */
    <T> T get(String key);

    /**
     * 获取缓存值,如果不存在则执行loader加载
     *
     * @param key    缓存key
     * @param loader 加载器
     * @return 缓存值
     */
    <T> T get(String key, Supplier<T> loader);

    /**
     * 获取缓存值,如果不存在则执行loader加载,并指定TTL
     *
     * @param key    缓存key
     * @param loader 加载器
     * @param ttl    过期时间
     * @param unit   时间单位
     * @return 缓存值
     */
    <T> T get(String key, Supplier<T> loader, long ttl, TimeUnit unit);

    /**
     * 设置缓存值
     *
     * @param key   缓存key
     * @param value 缓存值
     */
    void put(String key, Object value);

    /**
     * 设置缓存值,指定TTL
     *
     * @param key   缓存key
     * @param value 缓存值
     * @param ttl   过期时间
     * @param unit  时间单位
     */
    void put(String key, Object value, long ttl, TimeUnit unit);

    /**
     * 删除缓存
     *
     * @param key 缓存key
     */
    void evict(String key);

    /**
     * 删除多个缓存
     *
     * @param keys 缓存key数组
     */
    void evict(String... keys);

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 广播失效消息(通知其他节点删除本地缓存)
     *
     * @param key 缓存key
     */
    void broadcastEvict(String key);

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    CacheStats getStats();

    /**
     * 缓存统计信息
     */
    class CacheStats {
        private long l1HitCount;
        private long l1MissCount;
        private long l2HitCount;
        private long l2MissCount;
        private long totalHitCount;
        private long totalMissCount;
        private double totalHitRate;

        public CacheStats(long l1HitCount, long l1MissCount, long l2HitCount, long l2MissCount) {
            this.l1HitCount = l1HitCount;
            this.l1MissCount = l1MissCount;
            this.l2HitCount = l2HitCount;
            this.l2MissCount = l2MissCount;
            this.totalHitCount = l1HitCount + l2HitCount;
            this.totalMissCount = l1MissCount + l2MissCount;
            long total = totalHitCount + totalMissCount;
            this.totalHitRate = total > 0 ? (double) totalHitCount / total : 0.0;
        }

        // Getters
        public long getL1HitCount() { return l1HitCount; }
        public long getL1MissCount() { return l1MissCount; }
        public long getL2HitCount() { return l2HitCount; }
        public long getL2MissCount() { return l2MissCount; }
        public long getTotalHitCount() { return totalHitCount; }
        public long getTotalMissCount() { return totalMissCount; }
        public double getTotalHitRate() { return totalHitRate; }

        @Override
        public String toString() {
            return String.format("CacheStats{L1: %d/%d, L2: %d/%d, Total: %d/%d (%.2f%% hit rate)}",
                    l1HitCount, l1HitCount + l1MissCount,
                    l2HitCount, l2HitCount + l2MissCount,
                    totalHitCount, totalHitCount + totalMissCount,
                    totalHitRate * 100);
        }
    }
}
