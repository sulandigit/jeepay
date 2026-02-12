package com.jeequan.jeepay.core.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 多级缓存自动配置
 *
 * @author jeepay
 * @since 2.3.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MultiLevelCacheProperties.class)
@ConditionalOnProperty(prefix = "jeepay.cache.multi-level", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MultiLevelCacheAutoConfiguration {

    @Bean
    public IMultiLevelCacheManager multiLevelCacheManager(
            MultiLevelCacheProperties properties,
            RedisTemplate<String, Object> redisTemplate) {
        log.info("Initializing MultiLevelCacheManager...");
        return new MultiLevelCacheManager(properties, redisTemplate);
    }
}
