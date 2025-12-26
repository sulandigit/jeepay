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

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 统一线程池工厂
 * 负责创建和管理所有业务线程池，提供统一的配置入口和生命周期管理
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2024/10/10
 */
@Slf4j
@Component
public class ThreadPoolFactory {

    @Autowired
    private ThreadPoolProperties threadPoolProperties;

    @Autowired
    private ThreadPoolMetricsCollector metricsCollector;

    /**
     * 线程池实例缓存
     */
    private final ConcurrentHashMap<String, ThreadPoolTaskExecutor> executorMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (!threadPoolProperties.isEnabled()) {
            log.info("线程池统一管理功能已禁用");
            return;
        }

        log.info("线程池工厂初始化完成，开始创建预配置线程池");
        createPreconfiguredPools();
    }

    /**
     * 创建预配置的线程池
     */
    private void createPreconfiguredPools() {
        if (threadPoolProperties.getPools() != null) {
            threadPoolProperties.getPools().forEach((poolName, config) -> {
                try {
                    ThreadPoolTaskExecutor executor = createThreadPool(poolName, config);
                    executorMap.put(poolName, executor);
                    log.info("创建预配置线程池成功: {} - {}", poolName, config.getDescription());
                } catch (Exception e) {
                    log.error("创建预配置线程池失败: {}", poolName, e);
                }
            });
        }
    }

    /**
     * 获取线程池实例
     */
    public ThreadPoolTaskExecutor getExecutor(String poolName) {
        ThreadPoolTaskExecutor executor = executorMap.get(poolName);
        if (executor == null) {
            log.warn("线程池 {} 不存在，使用默认配置创建", poolName);
            executor = createDefaultThreadPool(poolName);
            executorMap.put(poolName, executor);
        }
        return executor;
    }

    /**
     * 创建线程池
     */
    public ThreadPoolTaskExecutor createThreadPool(String poolName) {
        // 查找具体配置
        ThreadPoolProperties.ThreadPoolConfig config = null;
        if (threadPoolProperties.getPools() != null) {
            config = threadPoolProperties.getPools().get(poolName);
        }

        if (config == null) {
            log.info("线程池 {} 无具体配置，使用默认配置", poolName);
            return createDefaultThreadPool(poolName);
        }

        return createThreadPool(poolName, config);
    }

    /**
     * 使用指定配置创建线程池
     */
    public ThreadPoolTaskExecutor createThreadPool(String poolName, ThreadPoolProperties.ThreadPoolConfig config) {
        // 合并配置（具体配置 > 业务类型配置 > 全局配置）
        ThreadPoolProperties.ThreadPoolConfig mergedConfig = mergeConfig(config);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 基础配置
        executor.setCorePoolSize(mergedConfig.getCorePoolSize());
        executor.setMaxPoolSize(mergedConfig.getMaxPoolSize());
        executor.setQueueCapacity(mergedConfig.getQueueCapacity());
        executor.setKeepAliveSeconds(mergedConfig.getKeepAliveSeconds());
        executor.setAllowCoreThreadTimeOut(mergedConfig.isAllowCoreThreadTimeOut());
        
        // 线程名前缀
        String prefix = mergedConfig.getThreadNamePrefix();
        if (prefix.endsWith("-")) {
            executor.setThreadNamePrefix(prefix);
        } else {
            executor.setThreadNamePrefix(prefix + "-" + poolName + "-");
        }
        
        // 拒绝策略
        RejectedExecutionHandler rejectedHandler = createRejectedExecutionHandler(
            poolName, mergedConfig.getRejectedExecutionHandler());
        executor.setRejectedExecutionHandler(rejectedHandler);

        // 初始化线程池
        executor.initialize();

        // 注册监控
        if (mergedConfig.isMonitorEnabled()) {
            metricsCollector.registerThreadPool(poolName, executor);
        }

        log.info("创建线程池 {} 成功 - core:{}, max:{}, queue:{}, keepAlive:{}s, reject:{}", 
            poolName, 
            mergedConfig.getCorePoolSize(),
            mergedConfig.getMaxPoolSize(),
            mergedConfig.getQueueCapacity(),
            mergedConfig.getKeepAliveSeconds(),
            mergedConfig.getRejectedExecutionHandler());

        return executor;
    }

    /**
     * 创建默认线程池
     */
    private ThreadPoolTaskExecutor createDefaultThreadPool(String poolName) {
        ThreadPoolProperties.ThreadPoolConfig defaultConfig = new ThreadPoolProperties.ThreadPoolConfig();
        copyGlobalConfig(threadPoolProperties.getGlobal(), defaultConfig);
        defaultConfig.setPoolName(poolName);
        defaultConfig.setDescription("默认配置线程池");
        
        return createThreadPool(poolName, defaultConfig);
    }

    /**
     * 合并配置
     */
    private ThreadPoolProperties.ThreadPoolConfig mergeConfig(ThreadPoolProperties.ThreadPoolConfig config) {
        ThreadPoolProperties.ThreadPoolConfig mergedConfig = new ThreadPoolProperties.ThreadPoolConfig();
        
        // 1. 先使用全局配置
        copyGlobalConfig(threadPoolProperties.getGlobal(), mergedConfig);
        
        // 2. 再使用业务类型配置
        if (config.getBusinessType() != null && threadPoolProperties.getBusinessTypes() != null) {
            ThreadPoolProperties.BusinessTypeConfig businessConfig = 
                threadPoolProperties.getBusinessTypes().get(config.getBusinessType());
            if (businessConfig != null) {
                copyGlobalConfig(businessConfig, mergedConfig);
            }
        }
        
        // 3. 最后使用具体配置
        copySpecificConfig(config, mergedConfig);
        
        return mergedConfig;
    }

    /**
     * 复制全局配置
     */
    private void copyGlobalConfig(ThreadPoolProperties.GlobalConfig source, ThreadPoolProperties.GlobalConfig target) {
        if (source.getCorePoolSize() > 0) target.setCorePoolSize(source.getCorePoolSize());
        if (source.getMaxPoolSize() > 0) target.setMaxPoolSize(source.getMaxPoolSize());
        if (source.getQueueCapacity() > 0) target.setQueueCapacity(source.getQueueCapacity());
        if (source.getKeepAliveSeconds() > 0) target.setKeepAliveSeconds(source.getKeepAliveSeconds());
        if (source.getRejectedExecutionHandler() != null) target.setRejectedExecutionHandler(source.getRejectedExecutionHandler());
        if (source.getThreadNamePrefix() != null) target.setThreadNamePrefix(source.getThreadNamePrefix());
        target.setAllowCoreThreadTimeOut(source.isAllowCoreThreadTimeOut());
    }

    /**
     * 复制具体配置
     */
    private void copySpecificConfig(ThreadPoolProperties.ThreadPoolConfig source, ThreadPoolProperties.ThreadPoolConfig target) {
        copyGlobalConfig(source, target);
        if (source.getPoolName() != null) target.setPoolName(source.getPoolName());
        if (source.getDescription() != null) target.setDescription(source.getDescription());
        if (source.getBusinessType() != null) target.setBusinessType(source.getBusinessType());
        target.setMonitorEnabled(source.isMonitorEnabled());
        if (source.getAlarm() != null) target.setAlarm(source.getAlarm());
    }

    /**
     * 创建拒绝执行处理器
     */
    private RejectedExecutionHandler createRejectedExecutionHandler(String poolName, String handlerName) {
        RejectedExecutionHandler originalHandler = ThreadPoolProperties.getRejectedExecutionHandler(handlerName);
        
        // 包装原始处理器，添加监控
        return (r, executor) -> {
            metricsCollector.recordRejectedTask(poolName);
            log.warn("线程池 {} 拒绝任务执行，当前活跃线程数: {}, 队列大小: {}", 
                poolName, executor.getActiveCount(), executor.getQueue().size());
            originalHandler.rejectedExecution(r, executor);
        };
    }

    /**
     * 获取所有线程池名称
     */
    public String[] getAllPoolNames() {
        return executorMap.keySet().toArray(new String[0]);
    }

    /**
     * 动态调整线程池参数
     */
    public void adjustThreadPool(String poolName, int corePoolSize, int maxPoolSize) {
        ThreadPoolTaskExecutor executor = executorMap.get(poolName);
        if (executor != null) {
            executor.setCorePoolSize(corePoolSize);
            executor.setMaxPoolSize(maxPoolSize);
            log.info("动态调整线程池 {} 参数: core={}, max={}", poolName, corePoolSize, maxPoolSize);
        } else {
            log.warn("线程池 {} 不存在，无法调整参数", poolName);
        }
    }

    /**
     * 优雅关闭线程池
     */
    public void shutdown() {
        log.info("开始关闭所有线程池...");
        executorMap.forEach((poolName, executor) -> {
            try {
                executor.shutdown();
                log.info("线程池 {} 关闭完成", poolName);
            } catch (Exception e) {
                log.error("关闭线程池 {} 时发生错误", poolName, e);
            }
        });
        executorMap.clear();
        log.info("所有线程池关闭完成");
    }
}