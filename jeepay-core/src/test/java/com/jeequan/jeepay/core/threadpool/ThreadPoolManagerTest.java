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
package com.jeequan.jeepay.core.threadpool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 线程池管理器单元测试
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2024/10/10
 */
@ExtendWith(MockitoExtension.class)
class ThreadPoolManagerTest {

    @Mock
    private ThreadPoolFactory threadPoolFactory;

    @Mock
    private ThreadPoolMetricsCollector metricsCollector;

    @Mock
    private ThreadPoolProperties threadPoolProperties;

    @Mock
    private ThreadPoolTaskExecutor mockExecutor;

    private ThreadPoolManager threadPoolManager;

    @BeforeEach
    void setUp() {
        threadPoolManager = new ThreadPoolManager();
        threadPoolManager.threadPoolFactory = threadPoolFactory;
        threadPoolManager.metricsCollector = metricsCollector;
        threadPoolManager.threadPoolProperties = threadPoolProperties;

        when(threadPoolProperties.isEnabled()).thenReturn(true);
    }

    @Test
    void testGetPaymentProcessPool() {
        // 测试获取支付处理线程池
        when(threadPoolFactory.getExecutor(ThreadPoolManager.ThreadPoolNames.PAYMENT_PROCESS_POOL))
                .thenReturn(mockExecutor);

        ThreadPoolTaskExecutor executor = threadPoolManager.getPaymentProcessPool();

        assertNotNull(executor);
        assertSame(mockExecutor, executor);
        verify(threadPoolFactory).getExecutor(ThreadPoolManager.ThreadPoolNames.PAYMENT_PROCESS_POOL);
    }

    @Test
    void testGetNotifyMerchantPool() {
        // 测试获取商户通知线程池
        when(threadPoolFactory.getExecutor(ThreadPoolManager.ThreadPoolNames.NOTIFY_MERCHANT_POOL))
                .thenReturn(mockExecutor);

        ThreadPoolTaskExecutor executor = threadPoolManager.getNotifyMerchantPool();

        assertNotNull(executor);
        assertSame(mockExecutor, executor);
        verify(threadPoolFactory).getExecutor(ThreadPoolManager.ThreadPoolNames.NOTIFY_MERCHANT_POOL);
    }

    @Test
    void testCreateCustomPool() {
        // 测试创建自定义线程池
        when(threadPoolFactory.createThreadPool(eq("customPool"), any(ThreadPoolProperties.ThreadPoolConfig.class)))
                .thenReturn(mockExecutor);

        ThreadPoolTaskExecutor executor = threadPoolManager.createCustomPool(
                "customPool", 5, 10, 100, "CallerRunsPolicy");

        assertNotNull(executor);
        assertSame(mockExecutor, executor);
        verify(threadPoolFactory).createThreadPool(eq("customPool"), any(ThreadPoolProperties.ThreadPoolConfig.class));
    }

    @Test
    void testGetPoolMetrics() {
        // 测试获取线程池监控指标
        ThreadPoolMetricsCollector.ThreadPoolMetrics mockMetrics = 
                new ThreadPoolMetricsCollector.ThreadPoolMetrics();
        when(metricsCollector.getMetrics("testPool")).thenReturn(mockMetrics);

        ThreadPoolMetricsCollector.ThreadPoolMetrics metrics = 
                threadPoolManager.getPoolMetrics("testPool");

        assertNotNull(metrics);
        assertSame(mockMetrics, metrics);
        verify(metricsCollector).getMetrics("testPool");
    }

    @Test
    void testGetAllPoolMetrics() {
        // 测试获取所有线程池监控指标
        Map<String, ThreadPoolMetricsCollector.ThreadPoolMetrics> mockMetricsMap = new HashMap<>();
        when(metricsCollector.getAllMetrics()).thenReturn(mockMetricsMap);

        Map<String, ThreadPoolMetricsCollector.ThreadPoolMetrics> metrics = 
                threadPoolManager.getAllPoolMetrics();

        assertNotNull(metrics);
        assertSame(mockMetricsMap, metrics);
        verify(metricsCollector).getAllMetrics();
    }

    @Test
    void testGetPoolOverview() {
        // 测试获取线程池状态概览
        String[] poolNames = {"pool1", "pool2"};
        when(threadPoolFactory.getAllPoolNames()).thenReturn(poolNames);

        Map<String, ThreadPoolMetricsCollector.ThreadPoolMetrics> metricsMap = new HashMap<>();
        ThreadPoolMetricsCollector.ThreadPoolMetrics metrics1 = new ThreadPoolMetricsCollector.ThreadPoolMetrics();
        metrics1.setActiveCount(5);
        metrics1.setQueueSize(10);
        metrics1.setCompletedTaskCount(100);
        metrics1.setRejectedTaskCount(2);

        ThreadPoolMetricsCollector.ThreadPoolMetrics metrics2 = new ThreadPoolMetricsCollector.ThreadPoolMetrics();
        metrics2.setActiveCount(3);
        metrics2.setQueueSize(5);
        metrics2.setCompletedTaskCount(50);
        metrics2.setRejectedTaskCount(1);

        metricsMap.put("pool1", metrics1);
        metricsMap.put("pool2", metrics2);
        when(metricsCollector.getAllMetrics()).thenReturn(metricsMap);

        Map<String, Object> overview = threadPoolManager.getPoolOverview();

        assertNotNull(overview);
        assertTrue((Boolean) overview.get("enabled"));
        assertEquals(2, overview.get("totalPools"));
        assertEquals(8, overview.get("totalActiveThreads")); // 5 + 3
        assertEquals(15, overview.get("totalQueuedTasks")); // 10 + 5
        assertEquals(150L, overview.get("totalCompletedTasks")); // 100 + 50
        assertEquals(3L, overview.get("totalRejectedTasks")); // 2 + 1
    }

    @Test
    void testCheckPoolHealth() {
        // 测试检查线程池健康状态
        Map<String, ThreadPoolMetricsCollector.ThreadPoolMetrics> metricsMap = new HashMap<>();
        
        // 创建健康的线程池指标
        ThreadPoolMetricsCollector.ThreadPoolMetrics healthyMetrics = new ThreadPoolMetricsCollector.ThreadPoolMetrics();
        healthyMetrics.setQueueSize(10);
        healthyMetrics.setQueueRemainingCapacity(90);
        healthyMetrics.setActiveCount(5);
        healthyMetrics.setPoolSize(10);
        healthyMetrics.setRejectedTaskCount(0);
        healthyMetrics.setAvgWaitTime(500);
        
        // 创建有问题的线程池指标
        ThreadPoolMetricsCollector.ThreadPoolMetrics problematicMetrics = new ThreadPoolMetricsCollector.ThreadPoolMetrics();
        problematicMetrics.setQueueSize(95);
        problematicMetrics.setQueueRemainingCapacity(5);
        problematicMetrics.setActiveCount(19);
        problematicMetrics.setPoolSize(20);
        problematicMetrics.setRejectedTaskCount(10);
        problematicMetrics.setAvgWaitTime(2000);

        metricsMap.put("healthyPool", healthyMetrics);
        metricsMap.put("problematicPool", problematicMetrics);
        when(metricsCollector.getAllMetrics()).thenReturn(metricsMap);

        Map<String, String> healthStatus = threadPoolManager.checkPoolHealth();

        assertNotNull(healthStatus);
        assertEquals("HEALTHY", healthStatus.get("healthyPool"));
        assertTrue(healthStatus.get("problematicPool").contains("WARNING") || 
                  healthStatus.get("problematicPool").contains("CRITICAL"));
    }

    @Test
    void testAdjustPoolSize() {
        // 测试动态调整线程池参数
        when(threadPoolFactory.adjustThreadPool("testPool", 10, 20)).thenReturn();

        boolean result = threadPoolManager.adjustPoolSize("testPool", 10, 20);

        assertTrue(result);
        verify(threadPoolFactory).adjustThreadPool("testPool", 10, 20);
    }

    @Test
    void testAdjustPoolSizeWithException() {
        // 测试调整线程池参数时发生异常
        doThrow(new RuntimeException("调整失败")).when(threadPoolFactory)
                .adjustThreadPool("testPool", 10, 20);

        boolean result = threadPoolManager.adjustPoolSize("testPool", 10, 20);

        assertFalse(result);
        verify(threadPoolFactory).adjustThreadPool("testPool", 10, 20);
    }
}