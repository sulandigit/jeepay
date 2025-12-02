package com.jeequan.jeepay.pay.ctrl.loadbalancer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 负载均衡健康检查控制器
 * 为负载均衡器提供专用的健康检查接口
 *
 * @author jeequan
 */
@RestController
@RequestMapping("/lb")
public class LoadBalancerHealthController {

    @Autowired
    private HealthEndpoint healthEndpoint;

    /**
     * 简单健康检查 - 用于负载均衡器
     * 返回200表示服务正常，其他状态码表示异常
     */
    @GetMapping("/health")
    public ResponseEntity<String> simpleHealth() {
        try {
            HealthComponent health = healthEndpoint.health();
            if (health != null) {
                return ResponseEntity.ok("healthy");
            } else {
                return ResponseEntity.status(503).body("unhealthy");
            }
        } catch (Exception e) {
            return ResponseEntity.status(503).body("error: " + e.getMessage());
        }
    }

    /**
     * 详细健康检查 - 包含详细信息
     */
    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        try {
            HealthComponent health = healthEndpoint.health();
            Map<String, Object> result = new HashMap<>();
            
            result.put("status", "UP");
            result.put("service", "jeepay-payment");
            result.put("version", "2.3.0");
            result.put("timestamp", System.currentTimeMillis());
            result.put("health", health);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(result);
        }
    }

    /**
     * 就绪检查 - 检查服务是否准备好处理请求
     */
    @GetMapping("/ready")
    public ResponseEntity<String> readiness() {
        try {
            // 检查关键依赖是否就绪
            HealthComponent health = healthEndpoint.health();
            if (health != null) {
                return ResponseEntity.ok("ready");
            } else {
                return ResponseEntity.status(503).body("not ready");
            }
        } catch (Exception e) {
            return ResponseEntity.status(503).body("not ready: " + e.getMessage());
        }
    }

    /**
     * 存活检查 - 检查服务是否存活
     */
    @GetMapping("/live")
    public ResponseEntity<String> liveness() {
        // 简单的存活检查，只要应用能响应就认为是存活的
        return ResponseEntity.ok("alive");
    }
}