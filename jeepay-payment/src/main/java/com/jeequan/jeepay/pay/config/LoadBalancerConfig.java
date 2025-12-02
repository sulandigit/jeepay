package com.jeequan.jeepay.pay.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

/**
 * 负载均衡配置
 * 配置Spring Cloud LoadBalancer
 *
 * @author jeequan
 */
@Configuration
@LoadBalancerClient(name = "jeepay-payment", configuration = LoadBalancerConfig.class)
public class LoadBalancerConfig {

    /**
     * 配置负载均衡的RestTemplate
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 自定义服务实例列表供应器
     * 可以实现自定义的负载均衡策略
     */
    @Bean
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
            Environment environment) {
        return ServiceInstanceListSupplier.builder()
                .withDiscoveryClient()
                .withHealthChecks()
                .withCaching()
                .build(environment);
    }
}