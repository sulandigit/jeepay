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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池告警服务
 * 监控线程池运行状态，触发告警通知
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2024/10/10
 */
@Slf4j
@Component
public class ThreadPoolAlarmService {

    @Autowired
    private ThreadPoolMetricsCollector metricsCollector;

    @Autowired
    private ThreadPoolProperties threadPoolProperties;

    /**
     * 告警状态记录
     */
    private final ConcurrentHashMap<String, AlarmState> alarmStateMap = new ConcurrentHashMap<>();

    /**
     * 告警调度器
     */
    private ScheduledExecutorService alarmScheduler;

    @PostConstruct
    public void init() {
        if (!threadPoolProperties.isEnabled()) {
            return;
        }

        alarmScheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "thread-pool-alarm");
            thread.setDaemon(true);
            return thread;
        });

        // 每分钟检查一次告警
        alarmScheduler.scheduleAtFixedRate(this::checkAlarms, 60, 60, TimeUnit.SECONDS);
        
        log.info("线程池告警服务初始化完成");
    }

    /**
     * 检查所有线程池告警
     */
    private void checkAlarms() {
        Map<String, ThreadPoolMetricsCollector.ThreadPoolMetrics> allMetrics = 
            metricsCollector.getAllMetrics();

        for (Map.Entry<String, ThreadPoolMetricsCollector.ThreadPoolMetrics> entry : allMetrics.entrySet()) {
            String poolName = entry.getKey();
            ThreadPoolMetricsCollector.ThreadPoolMetrics metrics = entry.getValue();
            
            try {
                checkPoolAlarms(poolName, metrics);
            } catch (Exception e) {
                log.error("检查线程池 {} 告警时发生错误", poolName, e);
            }
        }
    }

    /**
     * 检查单个线程池告警
     */
    private void checkPoolAlarms(String poolName, ThreadPoolMetricsCollector.ThreadPoolMetrics metrics) {
        // 获取配置的告警阈值
        ThreadPoolProperties.ThreadPoolConfig config = getPoolConfig(poolName);
        if (config == null || config.getAlarm() == null) {
            return;
        }

        ThreadPoolProperties.AlarmConfig alarmConfig = config.getAlarm();
        AlarmState alarmState = alarmStateMap.computeIfAbsent(poolName, k -> new AlarmState());

        // 检查队列使用率告警
        checkQueueUsageAlarm(poolName, metrics, alarmConfig, alarmState);
        
        // 检查活跃线程告警
        checkActiveThreadAlarm(poolName, metrics, alarmConfig, alarmState);
        
        // 检查任务拒绝告警
        checkRejectedTaskAlarm(poolName, metrics, alarmConfig, alarmState);
        
        // 检查平均等待时间告警
        checkAvgWaitTimeAlarm(poolName, metrics, alarmConfig, alarmState);
    }

    /**
     * 检查队列使用率告警
     */
    private void checkQueueUsageAlarm(String poolName, 
                                     ThreadPoolMetricsCollector.ThreadPoolMetrics metrics,
                                     ThreadPoolProperties.AlarmConfig alarmConfig,
                                     AlarmState alarmState) {
        double queueUsageRate = metrics.getQueueUsageRate();
        if (queueUsageRate > alarmConfig.getQueueUsageThreshold()) {
            if (!alarmState.isQueueUsageAlarmTriggered()) {
                triggerAlarm(poolName, AlarmType.QUEUE_USAGE, 
                    String.format("队列使用率过高: %.1f%% (阈值: %d%%)", 
                        queueUsageRate, alarmConfig.getQueueUsageThreshold()));
                alarmState.setQueueUsageAlarmTriggered(true);
            }
        } else {
            alarmState.setQueueUsageAlarmTriggered(false);
        }
    }

    /**
     * 检查活跃线程告警
     */
    private void checkActiveThreadAlarm(String poolName,
                                       ThreadPoolMetricsCollector.ThreadPoolMetrics metrics,
                                       ThreadPoolProperties.AlarmConfig alarmConfig,
                                       AlarmState alarmState) {
        double activeThreadRate = metrics.getActiveThreadRate();
        if (activeThreadRate > alarmConfig.getActiveThreadThreshold()) {
            if (!alarmState.isActiveThreadAlarmTriggered()) {
                triggerAlarm(poolName, AlarmType.ACTIVE_THREAD,
                    String.format("活跃线程比率过高: %.1f%% (阈值: %d%%)",
                        activeThreadRate, alarmConfig.getActiveThreadThreshold()));
                alarmState.setActiveThreadAlarmTriggered(true);
            }
        } else {
            alarmState.setActiveThreadAlarmTriggered(false);
        }
    }

    /**
     * 检查任务拒绝告警
     */
    private void checkRejectedTaskAlarm(String poolName,
                                       ThreadPoolMetricsCollector.ThreadPoolMetrics metrics,
                                       ThreadPoolProperties.AlarmConfig alarmConfig,
                                       AlarmState alarmState) {
        long rejectedTaskCount = metrics.getRejectedTaskCount();
        long lastRejectedCount = alarmState.getLastRejectedTaskCount();
        long newRejectedCount = rejectedTaskCount - lastRejectedCount;
        
        if (newRejectedCount > alarmConfig.getRejectedTaskThreshold()) {
            triggerAlarm(poolName, AlarmType.REJECTED_TASK,
                String.format("任务拒绝数量过多: %d (阈值: %d)", 
                    newRejectedCount, alarmConfig.getRejectedTaskThreshold()));
        }
        
        alarmState.setLastRejectedTaskCount(rejectedTaskCount);
    }

    /**
     * 检查平均等待时间告警
     */
    private void checkAvgWaitTimeAlarm(String poolName,
                                      ThreadPoolMetricsCollector.ThreadPoolMetrics metrics,
                                      ThreadPoolProperties.AlarmConfig alarmConfig,
                                      AlarmState alarmState) {
        double avgWaitTime = metrics.getAvgWaitTime();
        if (avgWaitTime > alarmConfig.getAvgWaitTimeThreshold()) {
            if (!alarmState.isAvgWaitTimeAlarmTriggered()) {
                triggerAlarm(poolName, AlarmType.AVG_WAIT_TIME,
                    String.format("平均等待时间过长: %.1fms (阈值: %dms)",
                        avgWaitTime, alarmConfig.getAvgWaitTimeThreshold()));
                alarmState.setAvgWaitTimeAlarmTriggered(true);
            }
        } else {
            alarmState.setAvgWaitTimeAlarmTriggered(false);
        }
    }

    /**
     * 触发告警
     */
    private void triggerAlarm(String poolName, AlarmType alarmType, String message) {
        String alarmMessage = String.format("[线程池告警] 线程池: %s, 类型: %s, 详情: %s", 
            poolName, alarmType.getDescription(), message);
        
        log.warn(alarmMessage);
        
        // TODO: 集成外部告警系统 (钉钉、邮件、短信等)
        sendAlarmNotification(poolName, alarmType, alarmMessage);
    }

    /**
     * 发送告警通知
     */
    private void sendAlarmNotification(String poolName, AlarmType alarmType, String message) {
        // 这里可以集成具体的告警通知实现
        // 例如：钉钉机器人、邮件、短信、企业微信等
        
        // 示例：记录告警日志
        log.warn("【线程池告警通知】{}", message);
        
        // 示例：可以在这里添加具体的通知实现
        // dingTalkService.sendAlarm(message);
        // emailService.sendAlarm(message);
        // smsService.sendAlarm(message);
    }

    /**
     * 获取线程池配置
     */
    private ThreadPoolProperties.ThreadPoolConfig getPoolConfig(String poolName) {
        if (threadPoolProperties.getPools() != null) {
            return threadPoolProperties.getPools().get(poolName);
        }
        return null;
    }

    /**
     * 告警类型枚举
     */
    public enum AlarmType {
        QUEUE_USAGE("队列使用率告警"),
        ACTIVE_THREAD("活跃线程告警"),
        REJECTED_TASK("任务拒绝告警"),
        AVG_WAIT_TIME("平均等待时间告警");

        private final String description;

        AlarmType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 告警状态
     */
    @Data
    private static class AlarmState {
        private boolean queueUsageAlarmTriggered = false;
        private boolean activeThreadAlarmTriggered = false;
        private boolean avgWaitTimeAlarmTriggered = false;
        private long lastRejectedTaskCount = 0;
    }
}