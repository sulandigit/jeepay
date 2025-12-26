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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程池监控指标收集器
 * 负责收集线程池运行时指标，包括容量、性能、健康状态等
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2024/10/10
 */
@Slf4j
@Component
public class ThreadPoolMetricsCollector {

    /**
     * 监控数据存储
     */
    private final ConcurrentHashMap<String, ThreadPoolMetrics> metricsMap = new ConcurrentHashMap<>();

    /**
     * 任务拒绝统计
     */
    private final ConcurrentHashMap<String, AtomicLong> rejectedTaskCountMap = new ConcurrentHashMap<>();

    /**
     * 任务执行时间统计
     */
    private final ConcurrentHashMap<String, TaskTimeMetrics> taskTimeMetricsMap = new ConcurrentHashMap<>();

    /**
     * 监控调度器
     */
    private ScheduledExecutorService monitorScheduler;

    @PostConstruct
    public void init() {
        monitorScheduler = new ScheduledThreadPoolExecutor(2, r -> {
            Thread thread = new Thread(r, "thread-pool-monitor");
            thread.setDaemon(true);
            return thread;
        });

        // 每30秒收集一次指标
        monitorScheduler.scheduleAtFixedRate(this::collectMetrics, 30, 30, TimeUnit.SECONDS);
        
        log.info("线程池监控指标收集器初始化完成");
    }

    /**
     * 注册线程池监控
     */
    public void registerThreadPool(String poolName, ThreadPoolTaskExecutor executor) {
        metricsMap.put(poolName, new ThreadPoolMetrics());
        rejectedTaskCountMap.put(poolName, new AtomicLong(0));
        taskTimeMetricsMap.put(poolName, new TaskTimeMetrics());
        
        log.info("注册线程池监控: {}", poolName);
    }

    /**
     * 记录任务拒绝
     */
    public void recordRejectedTask(String poolName) {
        AtomicLong counter = rejectedTaskCountMap.get(poolName);
        if (counter != null) {
            counter.incrementAndGet();
        }
    }

    /**
     * 记录任务执行时间
     */
    public void recordTaskExecutionTime(String poolName, long executionTime) {
        TaskTimeMetrics timeMetrics = taskTimeMetricsMap.get(poolName);
        if (timeMetrics != null) {
            timeMetrics.recordExecutionTime(executionTime);
        }
    }

    /**
     * 记录任务等待时间
     */
    public void recordTaskWaitTime(String poolName, long waitTime) {
        TaskTimeMetrics timeMetrics = taskTimeMetricsMap.get(poolName);
        if (timeMetrics != null) {
            timeMetrics.recordWaitTime(waitTime);
        }
    }

    /**
     * 获取线程池指标
     */
    public ThreadPoolMetrics getMetrics(String poolName) {
        return metricsMap.get(poolName);
    }

    /**
     * 获取所有线程池指标
     */
    public ConcurrentHashMap<String, ThreadPoolMetrics> getAllMetrics() {
        return new ConcurrentHashMap<>(metricsMap);
    }

    /**
     * 收集指标数据
     */
    private void collectMetrics() {
        metricsMap.forEach((poolName, metrics) -> {
            try {
                // 更新指标时间戳
                metrics.setLastUpdateTime(System.currentTimeMillis());
                
                // 更新拒绝任务数
                AtomicLong rejectedCount = rejectedTaskCountMap.get(poolName);
                if (rejectedCount != null) {
                    metrics.setRejectedTaskCount(rejectedCount.get());
                }

                // 更新任务时间指标
                TaskTimeMetrics timeMetrics = taskTimeMetricsMap.get(poolName);
                if (timeMetrics != null) {
                    metrics.setAvgExecutionTime(timeMetrics.getAvgExecutionTime());
                    metrics.setAvgWaitTime(timeMetrics.getAvgWaitTime());
                    metrics.setMaxExecutionTime(timeMetrics.getMaxExecutionTime());
                    metrics.setMaxWaitTime(timeMetrics.getMaxWaitTime());
                }

                log.debug("收集线程池 {} 指标: {}", poolName, metrics);
            } catch (Exception e) {
                log.error("收集线程池 {} 指标时发生错误", poolName, e);
            }
        });
    }

    /**
     * 线程池指标数据
     */
    @Data
    public static class ThreadPoolMetrics {
        /**
         * 核心线程数
         */
        private int corePoolSize;

        /**
         * 最大线程数
         */
        private int maxPoolSize;

        /**
         * 当前线程数
         */
        private int poolSize;

        /**
         * 活跃线程数
         */
        private int activeCount;

        /**
         * 队列大小
         */
        private int queueSize;

        /**
         * 队列剩余容量
         */
        private int queueRemainingCapacity;

        /**
         * 已完成任务数
         */
        private long completedTaskCount;

        /**
         * 总任务数
         */
        private long taskCount;

        /**
         * 拒绝任务数
         */
        private long rejectedTaskCount;

        /**
         * 平均执行时间（毫秒）
         */
        private double avgExecutionTime;

        /**
         * 平均等待时间（毫秒）
         */
        private double avgWaitTime;

        /**
         * 最大执行时间（毫秒）
         */
        private long maxExecutionTime;

        /**
         * 最大等待时间（毫秒）
         */
        private long maxWaitTime;

        /**
         * 最后更新时间
         */
        private long lastUpdateTime;

        /**
         * 队列使用率（百分比）
         */
        public double getQueueUsageRate() {
            int totalCapacity = queueSize + queueRemainingCapacity;
            return totalCapacity == 0 ? 0 : (double) queueSize / totalCapacity * 100;
        }

        /**
         * 线程池使用率（百分比）
         */
        public double getPoolUsageRate() {
            return maxPoolSize == 0 ? 0 : (double) poolSize / maxPoolSize * 100;
        }

        /**
         * 活跃线程比率（百分比）
         */
        public double getActiveThreadRate() {
            return poolSize == 0 ? 0 : (double) activeCount / poolSize * 100;
        }
    }

    /**
     * 任务时间指标
     */
    private static class TaskTimeMetrics {
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong totalWaitTime = new AtomicLong(0);
        private final AtomicLong executionCount = new AtomicLong(0);
        private final AtomicLong waitCount = new AtomicLong(0);
        private volatile long maxExecutionTime = 0;
        private volatile long maxWaitTime = 0;

        public void recordExecutionTime(long time) {
            totalExecutionTime.addAndGet(time);
            executionCount.incrementAndGet();
            updateMaxExecutionTime(time);
        }

        public void recordWaitTime(long time) {
            totalWaitTime.addAndGet(time);
            waitCount.incrementAndGet();
            updateMaxWaitTime(time);
        }

        private synchronized void updateMaxExecutionTime(long time) {
            if (time > maxExecutionTime) {
                maxExecutionTime = time;
            }
        }

        private synchronized void updateMaxWaitTime(long time) {
            if (time > maxWaitTime) {
                maxWaitTime = time;
            }
        }

        public double getAvgExecutionTime() {
            long count = executionCount.get();
            return count == 0 ? 0 : (double) totalExecutionTime.get() / count;
        }

        public double getAvgWaitTime() {
            long count = waitCount.get();
            return count == 0 ? 0 : (double) totalWaitTime.get() / count;
        }

        public long getMaxExecutionTime() {
            return maxExecutionTime;
        }

        public long getMaxWaitTime() {
            return maxWaitTime;
        }
    }
}