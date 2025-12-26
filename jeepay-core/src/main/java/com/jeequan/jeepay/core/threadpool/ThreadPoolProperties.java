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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置属性类
 * 支持全局默认配置、业务类型配置和具体线程池配置
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2024/10/10
 */
@Data
@Component
@ConfigurationProperties(prefix = "jeepay.thread-pool")
public class ThreadPoolProperties {

    /**
     * 是否启用线程池统一管理
     */
    private boolean enabled = true;

    /**
     * 全局默认配置
     */
    private GlobalConfig global = new GlobalConfig();

    /**
     * 业务类型配置
     */
    private Map<String, BusinessTypeConfig> businessTypes;

    /**
     * 具体线程池配置
     */
    private Map<String, ThreadPoolConfig> pools;

    /**
     * 全局默认配置
     */
    @Data
    public static class GlobalConfig {
        /**
         * 核心线程数
         */
        private int corePoolSize = 5;

        /**
         * 最大线程数
         */
        private int maxPoolSize = 20;

        /**
         * 队列容量
         */
        private int queueCapacity = 100;

        /**
         * 线程存活时间（秒）
         */
        private int keepAliveSeconds = 300;

        /**
         * 拒绝策略
         */
        private String rejectedExecutionHandler = "CallerRunsPolicy";

        /**
         * 线程名前缀
         */
        private String threadNamePrefix = "jeepay-";

        /**
         * 是否允许核心线程超时
         */
        private boolean allowCoreThreadTimeOut = false;
    }

    /**
     * 业务类型配置
     */
    @Data
    public static class BusinessTypeConfig extends GlobalConfig {
        /**
         * 业务类型描述
         */
        private String description;
    }

    /**
     * 具体线程池配置
     */
    @Data
    public static class ThreadPoolConfig extends GlobalConfig {
        /**
         * 线程池名称
         */
        private String poolName;

        /**
         * 线程池描述
         */
        private String description;

        /**
         * 所属业务类型
         */
        private String businessType;

        /**
         * 是否启用监控
         */
        private boolean monitorEnabled = true;

        /**
         * 告警阈值配置
         */
        private AlarmConfig alarm = new AlarmConfig();
    }

    /**
     * 告警配置
     */
    @Data
    public static class AlarmConfig {
        /**
         * 队列使用率告警阈值（百分比）
         */
        private int queueUsageThreshold = 80;

        /**
         * 活跃线程告警阈值（百分比）
         */
        private int activeThreadThreshold = 90;

        /**
         * 任务拒绝告警阈值（次数）
         */
        private int rejectedTaskThreshold = 10;

        /**
         * 平均等待时间告警阈值（毫秒）
         */
        private long avgWaitTimeThreshold = 1000;
    }

    /**
     * 获取拒绝策略实例
     */
    public static RejectedExecutionHandler getRejectedExecutionHandler(String handlerName) {
        switch (handlerName) {
            case "AbortPolicy":
                return new ThreadPoolExecutor.AbortPolicy();
            case "CallerRunsPolicy":
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case "DiscardPolicy":
                return new ThreadPoolExecutor.DiscardPolicy();
            case "DiscardOldestPolicy":
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            default:
                return new ThreadPoolExecutor.CallerRunsPolicy();
        }
    }
}