package com.jeequan.jeepay.pay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 分布式缓存配置
 * 配置Redis作为分布式缓存，支持负载均衡环境下的缓存共享
 *
 * @author jeequan
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.redis.time-to-live:1800000}") // 默认30分钟
    private long defaultTtl;

    /**
     * 配置Redis缓存管理器
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMillis(defaultTtl))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // 特定缓存配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 用户信息缓存 - 30分钟
        cacheConfigurations.put("userCache", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(30)));
        
        // 支付渠道配置缓存 - 1小时
        cacheConfigurations.put("channelCache", defaultCacheConfig
                .entryTtl(Duration.ofHours(1)));
        
        // 商户信息缓存 - 2小时
        cacheConfigurations.put("merchantCache", defaultCacheConfig
                .entryTtl(Duration.ofHours(2)));
        
        // 系统配置缓存 - 24小时
        cacheConfigurations.put("systemCache", defaultCacheConfig
                .entryTtl(Duration.ofHours(24)));
        
        // 短期缓存 - 5分钟
        cacheConfigurations.put("shortCache", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}