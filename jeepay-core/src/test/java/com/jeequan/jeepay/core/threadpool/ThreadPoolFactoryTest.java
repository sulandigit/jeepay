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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 线程池工厂单元测试
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2024/10/10
 */
@ExtendWith(MockitoExtension.class)
class ThreadPoolFactoryTest {

    @Mock
    private ThreadPoolProperties threadPoolProperties;

    @Mock
    private ThreadPoolMetricsCollector metricsCollector;

    private ThreadPoolFactory threadPoolFactory;

    @BeforeEach
    void setUp() {
        threadPoolFactory = new ThreadPoolFactory();
        threadPoolFactory.threadPoolProperties = threadPoolProperties;
        threadPoolFactory.metricsCollector = metricsCollector;

        // 模拟基础配置
        when(threadPoolProperties.isEnabled()).thenReturn(true);
        
        ThreadPoolProperties.GlobalConfig globalConfig = new ThreadPoolProperties.GlobalConfig();
        globalConfig.setCorePoolSize(5);
        globalConfig.setMaxPoolSize(10);
        globalConfig.setQueueCapacity(100);
        globalConfig.setKeepAliveSeconds(300);
        globalConfig.setRejectedExecutionHandler("CallerRunsPolicy");
        globalConfig.setThreadNamePrefix("test-");
        
        when(threadPoolProperties.getGlobal()).thenReturn(globalConfig);
    }

    @Test
    void testCreateDefaultThreadPool() {
        // 当没有具体配置时，应该创建默认线程池
        when(threadPoolProperties.getPools()).thenReturn(null);

        ThreadPoolTaskExecutor executor = threadPoolFactory.createThreadPool("testPool");

        assertNotNull(executor);
        assertEquals(5, executor.getCorePoolSize());
        assertEquals(10, executor.getMaxPoolSize());
        assertEquals(100, executor.getQueueCapacity());
        assertEquals(300, executor.getKeepAliveSeconds());
        
        // 验证监控注册
        verify(metricsCollector).registerThreadPool(eq("testPool"), eq(executor));
    }

    @Test
    void testCreateThreadPoolWithSpecificConfig() {
        // 测试使用具体配置创建线程池
        ThreadPoolProperties.ThreadPoolConfig config = new ThreadPoolProperties.ThreadPoolConfig();
        config.setPoolName("paymentPool");
        config.setCorePoolSize(20);
        config.setMaxPoolSize(100);
        config.setQueueCapacity(200);
        config.setKeepAliveSeconds(600);
        config.setRejectedExecutionHandler("AbortPolicy");
        config.setThreadNamePrefix("payment-");
        config.setMonitorEnabled(true);

        Map<String, ThreadPoolProperties.ThreadPoolConfig> pools = new HashMap<>();
        pools.put("paymentPool", config);
        when(threadPoolProperties.getPools()).thenReturn(pools);

        ThreadPoolTaskExecutor executor = threadPoolFactory.createThreadPool("paymentPool", config);

        assertNotNull(executor);
        assertEquals(20, executor.getCorePoolSize());
        assertEquals(100, executor.getMaxPoolSize());
        assertEquals(200, executor.getQueueCapacity());
        assertEquals(600, executor.getKeepAliveSeconds());
        
        verify(metricsCollector).registerThreadPool(eq("paymentPool"), eq(executor));
    }

    @Test
    void testGetExecutorCreateOnce() {
        // 测试获取线程池实例时只创建一次
        when(threadPoolProperties.getPools()).thenReturn(new HashMap<>());

        ThreadPoolTaskExecutor executor1 = threadPoolFactory.getExecutor("testPool");
        ThreadPoolTaskExecutor executor2 = threadPoolFactory.getExecutor("testPool");

        assertNotNull(executor1);
        assertNotNull(executor2);
        assertSame(executor1, executor2); // 应该是同一个实例
    }

    @Test
    void testAdjustThreadPool() {
        // 测试动态调整线程池参数
        when(threadPoolProperties.getPools()).thenReturn(new HashMap<>());
        
        ThreadPoolTaskExecutor executor = threadPoolFactory.getExecutor("testPool");
        
        threadPoolFactory.adjustThreadPool("testPool", 10, 20);
        
        assertEquals(10, executor.getCorePoolSize());
        assertEquals(20, executor.getMaxPoolSize());
    }

    @Test
    void testRejectedExecutionHandlerWithMonitoring() throws InterruptedException {
        // 测试拒绝策略中的监控记录
        ThreadPoolProperties.ThreadPoolConfig config = new ThreadPoolProperties.ThreadPoolConfig();
        config.setCorePoolSize(1);
        config.setMaxPoolSize(1);
        config.setQueueCapacity(1);
        config.setRejectedExecutionHandler("AbortPolicy");

        ThreadPoolTaskExecutor executor = threadPoolFactory.createThreadPool("testPool", config);
        
        // 填满线程池和队列
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        executor.execute(() -> {}); // 队列中的任务
        
        // 尝试提交第三个任务，应该被拒绝
        assertThrows(Exception.class, () -> {
            executor.execute(() -> {});
        });
        
        // 验证监控记录被调用
        verify(metricsCollector, atLeastOnce()).recordRejectedTask("testPool");
        
        latch.countDown();
    }
}