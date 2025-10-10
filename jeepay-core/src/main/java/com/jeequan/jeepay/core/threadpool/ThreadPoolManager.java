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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程池管理器
 * 提供线程池的统一管理接口，包括创建、监控、调整和销毁
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2024/10/10
 */
@Slf4j
@Component
public class ThreadPoolManager {

    @Autowired
    private ThreadPoolFactory threadPoolFactory;

    @Autowired
    private ThreadPoolMetricsCollector metricsCollector;

    @Autowired
    private ThreadPoolProperties threadPoolProperties;

    /**
     * 线程池常量定义
     */
    public static class ThreadPoolNames {
        // 核心业务线程池
        public static final String PAYMENT_PROCESS_POOL = "paymentProcessPool";
        public static final String REFUND_PROCESS_POOL = "refundProcessPool";
        public static final String NOTIFY_MERCHANT_POOL = "notifyMerchantPool";
        public static final String DIVISION_PROCESS_POOL = "divisionProcessPool";
        
        // 支撑服务线程池
        public static final String LOG_RECORD_POOL = "logRecordPool";
        public static final String CACHE_UPDATE_POOL = "cacheUpdatePool";
        public static final String FILE_UPLOAD_POOL = "fileUploadPool";
        
        // 定时任务线程池
        public static final String ORDER_REISSUE_POOL = "orderReissuePool";
        public static final String ORDER_EXPIRED_POOL = "orderExpiredPool";
        public static final String RECONCILIATION_POOL = "reconciliationPool";
        
        // MQ消息处理线程池
        public static final String MQ_MERCHANT_NOTIFY_POOL = "mqMerchantNotifyPool";
    }

    /**
     * 获取线程池执行器
     */
    public ThreadPoolTaskExecutor getExecutor(String poolName) {
        return threadPoolFactory.getExecutor(poolName);
    }

    /**
     * 获取支付处理线程池
     */
    public ThreadPoolTaskExecutor getPaymentProcessPool() {
        return getExecutor(ThreadPoolNames.PAYMENT_PROCESS_POOL);
    }

    /**
     * 获取退款处理线程池
     */
    public ThreadPoolTaskExecutor getRefundProcessPool() {
        return getExecutor(ThreadPoolNames.REFUND_PROCESS_POOL);
    }

    /**
     * 获取商户通知线程池
     */
    public ThreadPoolTaskExecutor getNotifyMerchantPool() {
        return getExecutor(ThreadPoolNames.NOTIFY_MERCHANT_POOL);
    }

    /**
     * 获取分账处理线程池
     */
    public ThreadPoolTaskExecutor getDivisionProcessPool() {
        return getExecutor(ThreadPoolNames.DIVISION_PROCESS_POOL);
    }

    /**
     * 获取日志记录线程池
     */
    public ThreadPoolTaskExecutor getLogRecordPool() {
        return getExecutor(ThreadPoolNames.LOG_RECORD_POOL);
    }

    /**
     * 获取缓存更新线程池
     */
    public ThreadPoolTaskExecutor getCacheUpdatePool() {
        return getExecutor(ThreadPoolNames.CACHE_UPDATE_POOL);
    }

    /**
     * 获取文件上传线程池
     */
    public ThreadPoolTaskExecutor getFileUploadPool() {
        return getExecutor(ThreadPoolNames.FILE_UPLOAD_POOL);
    }

    /**
     * 获取订单补单线程池
     */
    public ThreadPoolTaskExecutor getOrderReissuePool() {
        return getExecutor(ThreadPoolNames.ORDER_REISSUE_POOL);
    }

    /**
     * 获取订单过期处理线程池
     */
    public ThreadPoolTaskExecutor getOrderExpiredPool() {
        return getExecutor(ThreadPoolNames.ORDER_EXPIRED_POOL);
    }

    /**
     * 获取对账任务线程池
     */
    public ThreadPoolTaskExecutor getReconciliationPool() {
        return getExecutor(ThreadPoolNames.RECONCILIATION_POOL);
    }

    /**
     * 获取MQ商户通知线程池
     */
    public ThreadPoolTaskExecutor getMqMerchantNotifyPool() {
        return getExecutor(ThreadPoolNames.MQ_MERCHANT_NOTIFY_POOL);
    }

    /**
     * 创建自定义线程池
     */
    public ThreadPoolTaskExecutor createCustomPool(String poolName, 
                                                  int corePoolSize, 
                                                  int maxPoolSize, 
                                                  int queueCapacity,
                                                  String rejectedExecutionHandler) {
        ThreadPoolProperties.ThreadPoolConfig config = new ThreadPoolProperties.ThreadPoolConfig();
        config.setPoolName(poolName);
        config.setCorePoolSize(corePoolSize);
        config.setMaxPoolSize(maxPoolSize);
        config.setQueueCapacity(queueCapacity);
        config.setRejectedExecutionHandler(rejectedExecutionHandler);
        config.setDescription("自定义线程池");
        
        return threadPoolFactory.createThreadPool(poolName, config);
    }

    /**
     * 获取线程池监控指标
     */
    public ThreadPoolMetricsCollector.ThreadPoolMetrics getPoolMetrics(String poolName) {
        return metricsCollector.getMetrics(poolName);
    }

    /**
     * 获取所有线程池监控指标
     */
    public Map<String, ThreadPoolMetricsCollector.ThreadPoolMetrics> getAllPoolMetrics() {
        return metricsCollector.getAllMetrics();
    }

    /**
     * 获取线程池状态概览
     */
    public Map<String, Object> getPoolOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // 基本信息
        overview.put("enabled", threadPoolProperties.isEnabled());
        overview.put("totalPools", threadPoolFactory.getAllPoolNames().length);
        
        // 统计信息
        Map<String, ThreadPoolMetricsCollector.ThreadPoolMetrics> allMetrics = getAllPoolMetrics();
        int totalActiveThreads = 0;
        int totalQueuedTasks = 0;
        long totalCompletedTasks = 0;
        long totalRejectedTasks = 0;
        
        for (ThreadPoolMetricsCollector.ThreadPoolMetrics metrics : allMetrics.values()) {
            totalActiveThreads += metrics.getActiveCount();
            totalQueuedTasks += metrics.getQueueSize();
            totalCompletedTasks += metrics.getCompletedTaskCount();
            totalRejectedTasks += metrics.getRejectedTaskCount();
        }
        
        overview.put("totalActiveThreads", totalActiveThreads);
        overview.put("totalQueuedTasks", totalQueuedTasks);
        overview.put("totalCompletedTasks", totalCompletedTasks);
        overview.put("totalRejectedTasks", totalRejectedTasks);
        
        return overview;
    }

    /**
     * 检查线程池健康状态
     */
    public Map<String, String> checkPoolHealth() {
        Map<String, String> healthStatus = new HashMap<>();
        Map<String, ThreadPoolMetricsCollector.ThreadPoolMetrics> allMetrics = getAllPoolMetrics();
        
        for (Map.Entry<String, ThreadPoolMetricsCollector.ThreadPoolMetrics> entry : allMetrics.entrySet()) {
            String poolName = entry.getKey();
            ThreadPoolMetricsCollector.ThreadPoolMetrics metrics = entry.getValue();
            
            String status = "HEALTHY";
            
            // 检查队列使用率
            if (metrics.getQueueUsageRate() > 90) {
                status = "WARNING - 队列使用率过高: " + String.format("%.1f%%", metrics.getQueueUsageRate());
            }
            
            // 检查活跃线程比率
            if (metrics.getActiveThreadRate() > 95) {
                status = "WARNING - 活跃线程比率过高: " + String.format("%.1f%%", metrics.getActiveThreadRate());
            }
            
            // 检查拒绝任务数
            if (metrics.getRejectedTaskCount() > 0) {
                status = "CRITICAL - 存在拒绝任务: " + metrics.getRejectedTaskCount();
            }
            
            // 检查平均等待时间
            if (metrics.getAvgWaitTime() > 1000) {
                status = "WARNING - 平均等待时间过长: " + String.format("%.1fms", metrics.getAvgWaitTime());
            }
            
            healthStatus.put(poolName, status);
        }
        
        return healthStatus;
    }

    /**
     * 动态调整线程池参数
     */
    public boolean adjustPoolSize(String poolName, int corePoolSize, int maxPoolSize) {
        try {
            threadPoolFactory.adjustThreadPool(poolName, corePoolSize, maxPoolSize);
            log.info("动态调整线程池 {} 参数成功: core={}, max={}", poolName, corePoolSize, maxPoolSize);
            return true;
        } catch (Exception e) {
            log.error("动态调整线程池 {} 参数失败", poolName, e);
            return false;
        }
    }

    /**
     * 应用销毁时优雅关闭所有线程池
     */
    @PreDestroy
    public void destroy() {
        log.info("应用关闭，开始优雅关闭所有线程池...");
        threadPoolFactory.shutdown();
    }
}