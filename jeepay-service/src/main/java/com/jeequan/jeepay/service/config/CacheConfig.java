/*
 * Copyright (c) 2021-2031, 河北计全科技有限公司 (https://www.jeequan.com & jeequan@126.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeequan.jeepay.service.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.jeequan.jeepay.core.cache.CacheKeyManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Cache 配置类
 * 
 * 配置基于Redis的缓存管理器，定义各业务缓存空间及其过期时间
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025-10-15
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置RedisCacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 配置JSON序列化器
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // 默认缓存配置
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .entryTtl(Duration.ofMinutes(30)) // 默认30分钟过期
                .disableCachingNullValues(); // 不缓存null值

        // 为不同的缓存空间设置不同的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 商户信息缓存: 15分钟 (带随机偏移)
        cacheConfigurations.put("mchInfo", 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CacheKeyManager.TTL_MCH_INFO)));
        
        // 商户应用缓存: 30分钟 (带随机偏移)
        cacheConfigurations.put("mchApp", 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CacheKeyManager.TTL_MCH_APP)));
        
        // 商户应用配置上下文缓存: 30分钟
        cacheConfigurations.put("mchAppConfig", 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CacheKeyManager.TTL_MCH_APP_CONFIG)));
        
        // 服务商信息缓存: 15分钟 (带随机偏移)
        cacheConfigurations.put("isvInfo", 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CacheKeyManager.TTL_ISV_INFO)));
        
        // 服务商配置上下文缓存: 30分钟
        cacheConfigurations.put("isvConfig", 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CacheKeyManager.TTL_ISV_CONFIG)));
        
        // 支付接口配置缓存: 30分钟
        cacheConfigurations.put("payIfConfig", 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CacheKeyManager.TTL_PAY_IF_CONFIG)));
        
        // 系统配置缓存: 1小时 (带随机偏移)
        cacheConfigurations.put("sysConfig", 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CacheKeyManager.TTL_SYS_CONFIG)));
        
        // 用户权限信息缓存: 2小时
        cacheConfigurations.put("userPermission", 
                defaultCacheConfig.entryTtl(Duration.ofSeconds(CacheKeyManager.TTL_USER_PERMISSION)));

        // 空值缓存配置: 5分钟，允许缓存null值
        RedisCacheConfiguration nullValueConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer))
                .entryTtl(Duration.ofSeconds(CacheKeyManager.TTL_NULL_VALUE));
        
        cacheConfigurations.put("nullValue", nullValueConfig);

        // 构建RedisCacheManager
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
