package com.jeequan.jeepay.core.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多级缓存配置属性
 *
 * @author jeepay
 * @since 2.3.0
 */
@Data
@ConfigurationProperties(prefix = "jeepay.cache.multi-level")
public class MultiLevelCacheProperties {

    /**
     * 是否启用多级缓存
     */
    private Boolean enabled = true;

    /**
     * 缓存key前缀
     */
    private String keyPrefix = "jeepay";

    /**
     * 是否缓存null值
     */
    private Boolean cacheNullValues = true;

    /**
     * L1缓存(Caffeine)配置
     */
    private L1Config l1 = new L1Config();

    /**
     * L2缓存(Redis)配置
     */
    private L2Config l2 = new L2Config();

    /**
     * 统计配置
     */
    private StatsConfig stats = new StatsConfig();

    @Data
    public static class L1Config {
        /**
         * 是否启用L1缓存
         */
        private Boolean enabled = true;

        /**
         * 最大缓存条目数
         */
        private Integer maximumSize = 5000;

        /**
         * 写入后过期时间(分钟)
         */
        private Integer expireAfterWrite = 10;

        /**
         * 访问后过期时间(分钟)
         */
        private Integer expireAfterAccess = 30;

        /**
         * 初始容量
         */
        private Integer initialCapacity = 500;

        /**
         * 是否启用统计
         */
        private Boolean recordStats = true;
    }

    @Data
    public static class L2Config {
        /**
         * 是否启用L2缓存
         */
        private Boolean enabled = true;

        /**
         * 默认TTL(秒)
         */
        private Long defaultTtl = 1800L;
    }

    @Data
    public static class StatsConfig {
        /**
         * 是否启用统计
         */
        private Boolean enabled = true;

        /**
         * 统计间隔(秒)
         */
        private Integer interval = 60;
    }
}
