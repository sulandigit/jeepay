package com.jeequan.jeepay.pay.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 熔断器配置
 * 使用Resilience4j实现熔断器模式，提供故障转移和降级功能
 *
 * @author jeequan
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * 配置熔断器注册表
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    /**
     * 配置支付服务熔断器
     */
    @Bean
    public CircuitBreaker paymentCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                // 滑动窗口大小
                .slidingWindowSize(10)
                // 失败率阈值（百分比）
                .failureRateThreshold(50.0f)
                // 最小调用次数
                .minimumNumberOfCalls(5)
                // 在HALF_OPEN状态下允许的调用次数
                .permittedNumberOfCallsInHalfOpenState(3)
                // 从OPEN到HALF_OPEN状态的等待时间
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // 慢调用时间阈值
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                // 慢调用率阈值
                .slowCallRateThreshold(50.0f)
                // 记录异常作为失败
                .recordExceptions(Exception.class)
                // 忽略的异常
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        return registry.circuitBreaker("payment-service", config);
    }

    /**
     * 配置数据库熔断器
     */
    @Bean
    public CircuitBreaker databaseCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .failureRateThreshold(60.0f)
                .minimumNumberOfCalls(10)
                .permittedNumberOfCallsInHalfOpenState(5)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .slowCallRateThreshold(60.0f)
                .build();

        return registry.circuitBreaker("database-service", config);
    }

    /**
     * 配置Redis熔断器
     */
    @Bean
    public CircuitBreaker redisCircuitBreaker(CircuitBreakerRegistry registry) {
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowSize(15)
                .failureRateThreshold(40.0f)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .waitDurationInOpenState(Duration.ofSeconds(20))
                .slowCallDurationThreshold(Duration.ofSeconds(1))
                .slowCallRateThreshold(40.0f)
                .build();

        return registry.circuitBreaker("redis-service", config);
    }

    /**
     * 配置时间限制器注册表
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        return TimeLimiterRegistry.ofDefaults();
    }

    /**
     * 配置支付服务时间限制器
     */
    @Bean
    public TimeLimiter paymentTimeLimiter(TimeLimiterRegistry registry) {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(3))
                .cancelRunningFuture(true)
                .build();

        return registry.timeLimiter("payment-service", config);
    }

    /**
     * 配置数据库时间限制器
     */
    @Bean
    public TimeLimiter databaseTimeLimiter(TimeLimiterRegistry registry) {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build();

        return registry.timeLimiter("database-service", config);
    }
}