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
package com.jeequan.jeepay.pay.config;

import com.jeequan.jeepay.core.threadpool.ThreadPoolManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 定时任务线程池配置
 * 使用统一线程池管理框架优化定时任务执行
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2024/10/10
 */
@Slf4j
@Configuration
@EnableScheduling
public class ScheduledTaskConfig implements SchedulingConfigurer {

    @Autowired
    private ThreadPoolManager threadPoolManager;

    /**
     * 配置定时任务调度器
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());
    }

    /**
     * 定时任务调度器Bean
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        
        // 基于统一线程池管理的配置
        scheduler.setPoolSize(10); // 定时任务线程池大小
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        
        // 拒绝策略：记录日志并丢弃任务
        scheduler.setRejectedExecutionHandler((r, executor) -> {
            log.warn("定时任务被拒绝执行: {}, 当前活跃线程数: {}", 
                r.toString(), executor.getActiveCount());
        });
        
        scheduler.initialize();
        
        log.info("定时任务调度器初始化完成，线程池大小: {}", scheduler.getPoolSize());
        return scheduler;
    }

    /**
     * 订单补单任务执行器
     */
    @Bean("orderReissueTaskExecutor")
    public ThreadPoolTaskScheduler orderReissueTaskExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("order-reissue-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        
        log.info("订单补单任务执行器初始化完成");
        return scheduler;
    }

    /**
     * 订单过期处理任务执行器
     */
    @Bean("orderExpiredTaskExecutor")
    public ThreadPoolTaskScheduler orderExpiredTaskExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("order-expired-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        
        log.info("订单过期处理任务执行器初始化完成");
        return scheduler;
    }

    /**
     * 分账补单任务执行器
     */
    @Bean("divisionReissueTaskExecutor")
    public ThreadPoolTaskScheduler divisionReissueTaskExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("division-reissue-");
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        
        log.info("分账补单任务执行器初始化完成");
        return scheduler;
    }

}