package com.jeequan.jeepay.pay.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 自定义健康检查指示器
 * 检查数据库连接、Redis连接等关键依赖
 *
 * @author jeequan
 */
@Component("paymentServiceHealth")
public class PaymentServiceHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            // 检查数据库连接
            if (!checkDatabase()) {
                return Health.down()
                        .withDetail("database", "数据库连接失败")
                        .build();
            }

            // 检查Redis连接
            if (!checkRedis()) {
                return Health.down()
                        .withDetail("redis", "Redis连接失败")
                        .build();
            }

            // 检查业务逻辑
            if (!checkBusinessLogic()) {
                return Health.down()
                        .withDetail("business", "业务逻辑检查失败")
                        .build();
            }

            return Health.up()
                    .withDetail("database", "正常")
                    .withDetail("redis", "正常")
                    .withDetail("business", "正常")
                    .withDetail("service", "支付网关服务")
                    .withDetail("version", "2.3.0")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * 检查数据库连接
     */
    private boolean checkDatabase() {
        try {
            // 执行简单查询测试连接
            dataSource.getConnection().isValid(3);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查Redis连接
     */
    private boolean checkRedis() {
        try {
            // 执行ping命令测试连接
            String result = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            return "PONG".equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查业务逻辑
     */
    private boolean checkBusinessLogic() {
        try {
            // 这里可以添加特定的业务逻辑检查
            // 例如检查支付渠道配置、密钥是否正常等
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}