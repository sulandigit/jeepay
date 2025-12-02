package com.jeequan.jeepay.pay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * 会话管理配置
 * 使用Redis存储分布式会话，支持负载均衡环境下的会话共享
 *
 * @author jeequan
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600) // 会话过期时间1小时
public class SessionConfig {

    /**
     * 配置Cookie序列化器
     * 设置Cookie的属性，确保会话cookie的安全性
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        
        // Cookie名称
        serializer.setCookieName("JEEPAY_SESSION");
        
        // Cookie路径
        serializer.setCookiePath("/");
        
        // 域名设置 - 生产环境需要设置为实际域名
        // serializer.setDomainName(".jeepay.com");
        
        // HttpOnly设置，防止XSS攻击
        serializer.setUseHttpOnlyCookie(true);
        
        // Secure设置，HTTPS环境下启用
        serializer.setUseSecureCookie(false); // 开发环境设为false，生产环境设为true
        
        // SameSite设置
        serializer.setSameSite("Lax");
        
        // Cookie最大生命周期
        serializer.setCookieMaxAge(3600);
        
        return serializer;
    }

    /**
     * 配置Redis模板，用于会话数据序列化
     */
    @Bean
    public RedisTemplate<String, Object> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用String序列化器作为key序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 使用JSON序列化器作为value序列化器
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        
        return template;
    }
}