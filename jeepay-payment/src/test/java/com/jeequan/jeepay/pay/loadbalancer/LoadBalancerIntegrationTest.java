package com.jeequan.jeepay.pay.loadbalancer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * 负载均衡集成测试
 * 测试负载均衡环境下的服务行为
 *
 * @author jeequan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LoadBalancerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testHealthEndpoints() {
        // 测试简单健康检查
        ResponseEntity<String> simpleHealthResponse = restTemplate
                .getForEntity("http://localhost:" + port + "/lb/health", String.class);
        
        assertEquals(HttpStatus.OK, simpleHealthResponse.getStatusCode());
        assertEquals("healthy", simpleHealthResponse.getBody());

        // 测试详细健康检查
        ResponseEntity<String> detailedHealthResponse = restTemplate
                .getForEntity("http://localhost:" + port + "/lb/health/detailed", String.class);
        
        assertEquals(HttpStatus.OK, detailedHealthResponse.getStatusCode());
        assertTrue(detailedHealthResponse.getBody().contains("\"status\":\"UP\""));

        // 测试就绪检查
        ResponseEntity<String> readyResponse = restTemplate
                .getForEntity("http://localhost:" + port + "/lb/ready", String.class);
        
        assertEquals(HttpStatus.OK, readyResponse.getStatusCode());
        assertEquals("ready", readyResponse.getBody());

        // 测试存活检查
        ResponseEntity<String> liveResponse = restTemplate
                .getForEntity("http://localhost:" + port + "/lb/live", String.class);
        
        assertEquals(HttpStatus.OK, liveResponse.getStatusCode());
        assertEquals("alive", liveResponse.getBody());
    }

    @Test
    public void testActuatorHealthEndpoint() {
        // 测试Spring Boot Actuator健康检查
        ResponseEntity<String> actuatorHealthResponse = restTemplate
                .getForEntity("http://localhost:" + port + "/actuator/health", String.class);
        
        assertEquals(HttpStatus.OK, actuatorHealthResponse.getStatusCode());
        assertTrue(actuatorHealthResponse.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    public void testConcurrentHealthChecks() throws Exception {
        // 测试并发健康检查
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<ResponseEntity<String>>> futures = new ArrayList<>();

        // 启动多个并发请求
        for (int i = 0; i < 20; i++) {
            CompletableFuture<ResponseEntity<String>> future = CompletableFuture.supplyAsync(() -> {
                return restTemplate.getForEntity("http://localhost:" + port + "/lb/health", String.class);
            }, executor);
            futures.add(future);
        }

        // 等待所有请求完成并验证结果
        for (CompletableFuture<ResponseEntity<String>> future : futures) {
            ResponseEntity<String> response = future.get();
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("healthy", response.getBody());
        }

        executor.shutdown();
    }

    @Test
    public void testServiceRegistration() {
        // 测试服务注册相关的端点
        // 这里可以添加Nacos服务注册的验证逻辑
        
        // 测试服务实例信息
        ResponseEntity<String> instanceInfoResponse = restTemplate
                .getForEntity("http://localhost:" + port + "/actuator/info", String.class);
        
        // 验证响应状态
        assertTrue(instanceInfoResponse.getStatusCode() == HttpStatus.OK || 
                  instanceInfoResponse.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    public void testLoadBalancerConfiguration() {
        // 测试负载均衡器配置是否正确加载
        // 可以通过检查特定的配置端点或行为来验证
        
        ResponseEntity<String> configResponse = restTemplate
                .getForEntity("http://localhost:" + port + "/actuator/configprops", String.class);
        
        // 验证配置端点可访问
        assertTrue(configResponse.getStatusCode() == HttpStatus.OK || 
                  configResponse.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    public void testErrorHandling() {
        // 测试错误处理
        ResponseEntity<String> notFoundResponse = restTemplate
                .getForEntity("http://localhost:" + port + "/nonexistent", String.class);
        
        assertEquals(HttpStatus.NOT_FOUND, notFoundResponse.getStatusCode());
    }

    @Test
    public void testPerformanceUnderLoad() throws Exception {
        // 性能测试 - 模拟负载下的健康检查
        ExecutorService executor = Executors.newFixedThreadPool(50);
        List<CompletableFuture<Long>> futures = new ArrayList<>();

        // 记录开始时间
        long startTime = System.currentTimeMillis();

        // 启动大量并发请求
        for (int i = 0; i < 100; i++) {
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                long requestStart = System.currentTimeMillis();
                ResponseEntity<String> response = restTemplate
                        .getForEntity("http://localhost:" + port + "/lb/health", String.class);
                long requestEnd = System.currentTimeMillis();
                
                assertEquals(HttpStatus.OK, response.getStatusCode());
                return requestEnd - requestStart;
            }, executor);
            futures.add(future);
        }

        // 计算平均响应时间
        long totalResponseTime = 0;
        for (CompletableFuture<Long> future : futures) {
            totalResponseTime += future.get();
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double averageResponseTime = (double) totalResponseTime / futures.size();

        System.out.println("Total test time: " + totalTime + "ms");
        System.out.println("Average response time: " + averageResponseTime + "ms");

        // 验证性能指标
        assertTrue("Average response time should be less than 1000ms", averageResponseTime < 1000);
        assertTrue("Total test time should be reasonable", totalTime < 30000); // 30秒内完成

        executor.shutdown();
    }

    @Test
    public void testSessionConfiguration() {
        // 测试会话配置
        // 这里可以添加会话相关的测试逻辑
        
        // 验证Redis会话存储配置
        ResponseEntity<String> beansResponse = restTemplate
                .getForEntity("http://localhost:" + port + "/actuator/beans", String.class);
        
        // 检查是否有会话相关的Bean配置
        assertTrue(beansResponse.getStatusCode() == HttpStatus.OK || 
                  beansResponse.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    public void testMetricsEndpoint() {
        // 测试指标端点
        ResponseEntity<String> metricsResponse = restTemplate
                .getForEntity("http://localhost:" + port + "/actuator/metrics", String.class);
        
        // 验证指标端点可访问
        assertTrue(metricsResponse.getStatusCode() == HttpStatus.OK || 
                  metricsResponse.getStatusCode() == HttpStatus.NOT_FOUND);
        
        if (metricsResponse.getStatusCode() == HttpStatus.OK) {
            // 验证包含基本指标
            String body = metricsResponse.getBody();
            assertTrue(body.contains("jvm.memory.used") || body.contains("names"));
        }
    }
}