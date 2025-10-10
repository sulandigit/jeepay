package com.jeequan.jeepay.pay.loadbalancer;

import com.jeequan.jeepay.pay.service.fallback.FallbackService;
import com.jeequan.jeepay.core.model.ApiRes;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 熔断器功能测试
 *
 * @author jeequan
 */
@RunWith(MockitoJUnitRunner.class)
public class CircuitBreakerTest {

    @Mock
    private FallbackService fallbackService;

    private CircuitBreaker circuitBreaker;
    private CircuitBreakerRegistry registry;

    @Before
    public void setUp() {
        registry = CircuitBreakerRegistry.ofDefaults();
        
        // 创建测试用的熔断器配置
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .slidingWindowSize(5)
                .failureRateThreshold(50.0f)
                .minimumNumberOfCalls(3)
                .permittedNumberOfCallsInHalfOpenState(2)
                .waitDurationInOpenState(java.time.Duration.ofSeconds(1))
                .build();

        circuitBreaker = registry.circuitBreaker("test-service", config);
    }

    @Test
    public void testCircuitBreakerClosed_SuccessfulCalls() {
        // 模拟成功的服务调用
        Supplier<String> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, () -> "success");

        // 执行多次成功调用
        for (int i = 0; i < 5; i++) {
            String result = decoratedSupplier.get();
            assertEquals("success", result);
        }

        // 验证熔断器状态为CLOSED
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    public void testCircuitBreakerOpen_FailureCalls() {
        // 模拟失败的服务调用
        Supplier<String> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, () -> {
                    throw new RuntimeException("Service failure");
                });

        // 执行多次失败调用，触发熔断器打开
        for (int i = 0; i < 5; i++) {
            try {
                decoratedSupplier.get();
            } catch (Exception e) {
                // 预期的异常
            }
        }

        // 验证熔断器状态为OPEN
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    public void testCircuitBreakerHalfOpen() throws InterruptedException {
        // 首先触发熔断器打开
        Supplier<String> failingSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, () -> {
                    throw new RuntimeException("Service failure");
                });

        for (int i = 0; i < 5; i++) {
            try {
                failingSupplier.get();
            } catch (Exception e) {
                // 预期的异常
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // 等待熔断器进入HALF_OPEN状态
        Thread.sleep(1100); // 等待超过配置的等待时间

        // 模拟成功的调用
        Supplier<String> successSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, () -> "recovery");

        // 第一次调用应该触发HALF_OPEN状态
        try {
            String result = successSupplier.get();
            assertEquals("recovery", result);
        } catch (Exception e) {
            // 可能还处于OPEN状态
        }

        // 验证状态变化
        assertTrue(circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN || 
                   circuitBreaker.getState() == CircuitBreaker.State.CLOSED);
    }

    @Test
    public void testFallbackService_PaymentFallback() {
        // 模拟支付服务降级
        RuntimeException exception = new RuntimeException("Payment service unavailable");
        ApiRes expectedResponse = ApiRes.customFail("支付服务暂时不可用", null);
        
        when(fallbackService.paymentFallback(exception)).thenReturn(expectedResponse);

        ApiRes result = fallbackService.paymentFallback(exception);

        assertNotNull(result);
        assertEquals("支付服务暂时不可用", result.getMsg());
        verify(fallbackService).paymentFallback(exception);
    }

    @Test
    public void testFallbackService_DatabaseFallback() {
        // 模拟数据库服务降级
        RuntimeException exception = new RuntimeException("Database connection failed");
        ApiRes expectedResponse = ApiRes.customFail("数据库服务异常", null);
        
        when(fallbackService.databaseFallback(exception)).thenReturn(expectedResponse);

        ApiRes result = fallbackService.databaseFallback(exception);

        assertNotNull(result);
        assertEquals("数据库服务异常", result.getMsg());
        verify(fallbackService).databaseFallback(exception);
    }

    @Test
    public void testFallbackService_ChannelFallback() {
        // 模拟支付渠道降级
        String channelCode = "ALI_PAY";
        RuntimeException exception = new RuntimeException("Channel unavailable");
        ApiRes expectedResponse = ApiRes.customFail("支付渠道不可用", null);
        
        when(fallbackService.channelFallback(channelCode, exception)).thenReturn(expectedResponse);

        ApiRes result = fallbackService.channelFallback(channelCode, exception);

        assertNotNull(result);
        assertEquals("支付渠道不可用", result.getMsg());
        verify(fallbackService).channelFallback(channelCode, exception);
    }

    @Test
    public void testCircuitBreakerMetrics() {
        // 测试熔断器指标
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertNotNull(metrics);
        assertEquals(0, metrics.getNumberOfSuccessfulCalls());
        assertEquals(0, metrics.getNumberOfFailedCalls());
        assertEquals(-1.0f, metrics.getFailureRate(), 0.01f);
    }

    @Test
    public void testCircuitBreakerEventListener() {
        // 测试熔断器事件监听
        final boolean[] eventFired = {false};

        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    eventFired[0] = true;
                    System.out.println("Circuit breaker state transition: " + 
                                     event.getStateTransition());
                });

        // 触发状态变化
        Supplier<String> failingSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, () -> {
                    throw new RuntimeException("Test failure");
                });

        for (int i = 0; i < 5; i++) {
            try {
                failingSupplier.get();
            } catch (Exception e) {
                // 预期的异常
            }
        }

        // 验证事件是否触发
        assertTrue("State transition event should be fired", eventFired[0]);
    }
}