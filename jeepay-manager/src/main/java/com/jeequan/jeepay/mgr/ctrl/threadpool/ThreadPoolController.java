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
package com.jeequan.jeepay.mgr.ctrl.threadpool;

import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.threadpool.ThreadPoolManager;
import com.jeequan.jeepay.core.threadpool.ThreadPoolMetricsCollector;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 线程池动态管理控制器
 * 提供线程池监控、调整和管理功能
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2024/10/10
 */
@Api(tags = "系统管理（线程池管理）")
@Slf4j
@RestController
@RequestMapping("api/threadPool")
public class ThreadPoolController extends CommonCtrl {

    @Autowired
    private ThreadPoolManager threadPoolManager;

    /**
     * 获取线程池概览信息
     */
    @ApiOperation("获取线程池概览")
    @MethodLog(remark = "获取线程池概览")
    @PreAuthorize("hasAuthority('ENT_THREAD_POOL_VIEW')")
    @GetMapping("/overview")
    public ApiRes getOverview() {
        try {
            Map<String, Object> overview = threadPoolManager.getPoolOverview();
            return ApiRes.ok(overview);
        } catch (Exception e) {
            log.error("获取线程池概览失败", e);
            return ApiRes.fail(ApiCodeEnum.SYSTEM_ERROR, "获取线程池概览失败");
        }
    }

    /**
     * 获取所有线程池监控指标
     */
    @ApiOperation("获取线程池监控指标")
    @MethodLog(remark = "获取线程池监控指标")
    @PreAuthorize("hasAuthority('ENT_THREAD_POOL_VIEW')")
    @GetMapping("/metrics")
    public ApiRes getAllMetrics() {
        try {
            Map<String, ThreadPoolMetricsCollector.ThreadPoolMetrics> metrics = 
                threadPoolManager.getAllPoolMetrics();
            return ApiRes.ok(metrics);
        } catch (Exception e) {
            log.error("获取线程池监控指标失败", e);
            return ApiRes.fail(ApiCodeEnum.SYSTEM_ERROR, "获取监控指标失败");
        }
    }

    /**
     * 获取指定线程池监控指标
     */
    @ApiOperation("获取指定线程池监控指标")
    @ApiImplicitParam(name = "poolName", value = "线程池名称", required = true)
    @MethodLog(remark = "获取指定线程池监控指标")
    @PreAuthorize("hasAuthority('ENT_THREAD_POOL_VIEW')")
    @GetMapping("/metrics/{poolName}")
    public ApiRes getPoolMetrics(@PathVariable String poolName) {
        try {
            ThreadPoolMetricsCollector.ThreadPoolMetrics metrics = 
                threadPoolManager.getPoolMetrics(poolName);
            if (metrics == null) {
                return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE, "线程池不存在");
            }
            return ApiRes.ok(metrics);
        } catch (Exception e) {
            log.error("获取线程池 {} 监控指标失败", poolName, e);
            return ApiRes.fail(ApiCodeEnum.SYSTEM_ERROR, "获取监控指标失败");
        }
    }

    /**
     * 检查线程池健康状态
     */
    @ApiOperation("检查线程池健康状态")
    @MethodLog(remark = "检查线程池健康状态")
    @PreAuthorize("hasAuthority('ENT_THREAD_POOL_VIEW')")
    @GetMapping("/health")
    public ApiRes checkHealth() {
        try {
            Map<String, String> healthStatus = threadPoolManager.checkPoolHealth();
            return ApiRes.ok(healthStatus);
        } catch (Exception e) {
            log.error("检查线程池健康状态失败", e);
            return ApiRes.fail(ApiCodeEnum.SYSTEM_ERROR, "健康检查失败");
        }
    }

    /**
     * 动态调整线程池参数
     */
    @ApiOperation("动态调整线程池参数")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "poolName", value = "线程池名称", required = true),
        @ApiImplicitParam(name = "corePoolSize", value = "核心线程数", required = true),
        @ApiImplicitParam(name = "maxPoolSize", value = "最大线程数", required = true)
    })
    @MethodLog(remark = "动态调整线程池参数")
    @PreAuthorize("hasAuthority('ENT_THREAD_POOL_EDIT')")
    @PostMapping("/adjust")
    public ApiRes adjustPool(@RequestBody @Validated ThreadPoolAdjustRequest request) {
        try {
            boolean success = threadPoolManager.adjustPoolSize(
                request.getPoolName(), 
                request.getCorePoolSize(), 
                request.getMaxPoolSize()
            );
            
            if (success) {
                return ApiRes.ok("线程池参数调整成功");
            } else {
                return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_UPDATE, "线程池参数调整失败");
            }
        } catch (Exception e) {
            log.error("动态调整线程池 {} 参数失败", request.getPoolName(), e);
            return ApiRes.fail(ApiCodeEnum.SYSTEM_ERROR, "参数调整失败: " + e.getMessage());
        }
    }

    /**
     * 线程池参数调整请求对象
     */
    @Data
    public static class ThreadPoolAdjustRequest {
        @NotBlank(message = "线程池名称不能为空")
        private String poolName;

        @Min(value = 1, message = "核心线程数至少为1")
        @Max(value = 1000, message = "核心线程数不能超过1000")
        private int corePoolSize;

        @Min(value = 1, message = "最大线程数至少为1")
        @Max(value = 2000, message = "最大线程数不能超过2000")
        private int maxPoolSize;
    }

}