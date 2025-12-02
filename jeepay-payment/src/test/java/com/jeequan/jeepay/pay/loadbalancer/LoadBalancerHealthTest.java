package com.jeequan.jeepay.pay.loadbalancer;

import com.jeequan.jeepay.pay.ctrl.loadbalancer.LoadBalancerHealthController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 负载均衡健康检查测试
 *
 * @author jeequan
 */
@RunWith(SpringRunner.class)
@WebMvcTest(LoadBalancerHealthController.class)
public class LoadBalancerHealthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthEndpoint healthEndpoint;

    @Test
    public void testSimpleHealthCheck_Healthy() throws Exception {
        // 模拟健康状态
        SystemHealth health = SystemHealth.up().build();
        when(healthEndpoint.health()).thenReturn(health);

        mockMvc.perform(get("/lb/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("healthy"));
    }

    @Test
    public void testSimpleHealthCheck_Unhealthy() throws Exception {
        // 模拟健康检查返回null
        when(healthEndpoint.health()).thenReturn(null);

        mockMvc.perform(get("/lb/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("unhealthy"));
    }

    @Test
    public void testSimpleHealthCheck_Exception() throws Exception {
        // 模拟异常情况
        when(healthEndpoint.health()).thenThrow(new RuntimeException("Health check failed"));

        mockMvc.perform(get("/lb/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("error: Health check failed"));
    }

    @Test
    public void testDetailedHealth() throws Exception {
        // 模拟健康状态
        SystemHealth health = SystemHealth.up().build();
        when(healthEndpoint.health()).thenReturn(health);

        mockMvc.perform(get("/lb/health/detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("jeepay-payment"))
                .andExpect(jsonPath("$.version").value("2.3.0"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.health").exists());
    }

    @Test
    public void testDetailedHealth_Exception() throws Exception {
        // 模拟异常情况
        when(healthEndpoint.health()).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/lb/health/detailed"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"))
                .andExpect(jsonPath("$.error").value("Service error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testReadinessCheck() throws Exception {
        // 模拟健康状态
        SystemHealth health = SystemHealth.up().build();
        when(healthEndpoint.health()).thenReturn(health);

        mockMvc.perform(get("/lb/ready"))
                .andExpect(status().isOk())
                .andExpect(content().string("ready"));
    }

    @Test
    public void testReadinessCheck_NotReady() throws Exception {
        // 模拟不健康状态
        when(healthEndpoint.health()).thenReturn(null);

        mockMvc.perform(get("/lb/ready"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("not ready"));
    }

    @Test
    public void testLivenessCheck() throws Exception {
        // 存活检查应该总是返回成功
        mockMvc.perform(get("/lb/live"))
                .andExpect(status().isOk())
                .andExpect(content().string("alive"));
    }
}