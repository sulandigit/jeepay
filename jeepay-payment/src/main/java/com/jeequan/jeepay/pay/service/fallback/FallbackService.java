package com.jeequan.jeepay.pay.service.fallback;

import com.jeequan.jeepay.core.model.ApiRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 降级处理服务
 * 当服务不可用时提供备用响应
 *
 * @author jeequan
 */
@Slf4j
@Service
public class FallbackService {

    /**
     * 支付服务降级处理
     */
    public ApiRes paymentFallback(Exception ex) {
        log.warn("支付服务降级处理，原因: {}", ex.getMessage());
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "支付服务暂时不可用，请稍后重试");
        data.put("errorCode", "SERVICE_UNAVAILABLE");
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiRes.customFail("支付服务暂时不可用", data);
    }

    /**
     * 查询服务降级处理
     */
    public ApiRes queryFallback(Exception ex) {
        log.warn("查询服务降级处理，原因: {}", ex.getMessage());
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "查询服务暂时不可用，请稍后重试");
        data.put("errorCode", "QUERY_SERVICE_UNAVAILABLE");
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiRes.customFail("查询服务暂时不可用", data);
    }

    /**
     * 数据库服务降级处理
     */
    public ApiRes databaseFallback(Exception ex) {
        log.error("数据库服务降级处理，原因: {}", ex.getMessage());
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "数据库连接异常，请联系管理员");
        data.put("errorCode", "DATABASE_ERROR");
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiRes.customFail("数据库服务异常", data);
    }

    /**
     * Redis服务降级处理
     */
    public Map<String, Object> redisFallback(Exception ex) {
        log.warn("Redis服务降级处理，原因: {}", ex.getMessage());
        
        Map<String, Object> result = new HashMap<>();
        result.put("cached", false);
        result.put("source", "fallback");
        result.put("message", "缓存服务不可用，已降级到数据库查询");
        
        return result;
    }

    /**
     * 第三方支付渠道降级处理
     */
    public ApiRes channelFallback(String channelCode, Exception ex) {
        log.error("支付渠道[{}]降级处理，原因: {}", channelCode, ex.getMessage());
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "支付渠道暂时不可用，请选择其他支付方式");
        data.put("channelCode", channelCode);
        data.put("errorCode", "CHANNEL_UNAVAILABLE");
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiRes.customFail("支付渠道不可用", data);
    }

    /**
     * 通用服务降级处理
     */
    public ApiRes genericFallback(String serviceName, Exception ex) {
        log.warn("服务[{}]降级处理，原因: {}", serviceName, ex.getMessage());
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", serviceName + "服务暂时不可用，请稍后重试");
        data.put("serviceName", serviceName);
        data.put("errorCode", "SERVICE_FALLBACK");
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiRes.customFail("服务暂时不可用", data);
    }

    /**
     * 系统维护模式响应
     */
    public ApiRes maintenanceMode() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "系统正在维护中，预计维护时间30分钟");
        data.put("errorCode", "SYSTEM_MAINTENANCE");
        data.put("estimatedTime", "30分钟");
        data.put("timestamp", System.currentTimeMillis());
        
        return ApiRes.customFail("系统维护中", data);
    }
}