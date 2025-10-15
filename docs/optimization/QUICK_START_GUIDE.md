# 数据库查询优化 - 快速开始指南

## 前置条件

- MySQL 5.7+
- Redis 5.0+
- JDK 1.8+
- 已部署运行的Jeepay系统

---

## 快速部署步骤

### 第一步：执行索引创建脚本（5分钟）

**重要**: 请在业务低峰期执行，建议凌晨进行

1. 连接到数据库:
```bash
mysql -u root -p jeepaydb
```

2. 执行索引创建脚本:
```sql
-- 1. 支付订单表索引
source /path/to/jeepay/docs/sql/optimization/001_add_pay_order_indexes.sql

-- 2. 退款订单表索引
source /path/to/jeepay/docs/sql/optimization/002_add_refund_order_indexes.sql

-- 3. 转账订单和通知记录表索引
source /path/to/jeepay/docs/sql/optimization/003_add_transfer_notify_indexes.sql
```

3. 验证索引创建成功:
```sql
SHOW INDEX FROM t_pay_order;
SHOW INDEX FROM t_refund_order;
SHOW INDEX FROM t_transfer_order;
SHOW INDEX FROM t_mch_notify_record;
```

---

### 第二步：更新配置文件（2分钟）

配置文件已优化完成，位于 `conf/devCommons/config/application.yml`

主要优化项:
- 数据库连接池参数调优
- 慢SQL阈值降低至2000ms

**无需手动操作，配置已更新**

---

### 第三步：验证Redis服务（1分钟）

确保Redis服务正常运行:

```bash
# 检查Redis状态
redis-cli ping
# 应返回: PONG

# 检查Redis配置
redis-cli INFO server
```

如果Redis未启动:
```bash
# 启动Redis
redis-server

# 或使用Docker启动
docker run -d -p 6379:6379 redis:latest
```

---

### 第四步：重启应用（3分钟）

1. 停止应用:
```bash
# 根据部署方式选择停止命令
# 方式1: 使用脚本
./shutdown.sh

# 方式2: 直接kill进程
ps -ef | grep jeepay
kill -15 <pid>
```

2. 启动应用:
```bash
# 使用脚本启动
./startup.sh

# 或使用Docker启动
docker-compose up -d
```

3. 检查日志:
```bash
tail -f logs/jeepay-payment.log
```

确认无报错信息，看到类似以下日志表示启动成功:
```
Started JeepayApplication in xx seconds
```

---

### 第五步：功能验证（5分钟）

#### 5.1 验证索引使用

登录数据库执行:
```sql
-- 查看订单查询执行计划
EXPLAIN SELECT * FROM t_pay_order 
WHERE mch_no = 'M001' AND state = 2 
ORDER BY created_at DESC LIMIT 20;
```

检查结果中的 `key` 列，应显示 `idx_mch_state_created`

#### 5.2 验证缓存功能

查看Redis缓存:
```bash
# 监控缓存写入
redis-cli MONITOR | grep "jeepay:pay"
```

然后在系统中查询一个订单，应该能看到类似以下输出:
```
"SET" "jeepay:pay:order:P202501150001" ...
```

#### 5.3 验证游标分页（可选）

如果前端已适配游标分页，测试翻页功能，确认正常工作。

---

## 性能对比测试

### 测试前准备

1. 准备测试数据（建议100万条以上）
2. 清空Redis缓存: `redis-cli FLUSHALL`
3. 重启MySQL以清除查询缓存: `service mysql restart`

### 测试场景1: 订单查询

```sql
-- 优化前后对比
-- 查询商户订单列表（第1页）
SELECT * FROM t_pay_order 
WHERE mch_no = 'M001' AND state = 2 
ORDER BY created_at DESC LIMIT 20;

-- 查询商户订单列表（第100页）
SELECT * FROM t_pay_order 
WHERE mch_no = 'M001' AND state = 2 
ORDER BY created_at DESC LIMIT 2000, 20;
```

记录执行时间，对比优化前后的性能提升。

### 测试场景2: 统计查询

```sql
-- 日统计查询
SELECT 
    DATE(created_at) as groupDate,
    ROUND(IFNULL(SUM(amount) - SUM(refund_amount), 0)/100, 2) AS payAmount,
    ROUND(IFNULL(SUM(refund_amount), 0)/100, 2) AS refundAmount
FROM t_pay_order
WHERE state IN (2, 5)
  AND mch_no = 'M001'
  AND created_at >= '2025-01-01 00:00:00'
  AND created_at <= '2025-01-31 23:59:59'
GROUP BY DATE(created_at)
ORDER BY DATE(created_at) DESC;
```

### 测试场景3: 缓存命中测试

```bash
# 第一次查询（缓存未命中）
curl "http://localhost:9217/api/pay/order/P202501150001"

# 第二次查询（缓存命中）
curl "http://localhost:9217/api/pay/order/P202501150001"

# 查看缓存命中情况
redis-cli INFO stats | grep keyspace
```

---

## 监控和观察

### Druid监控面板

访问: `http://localhost:9217/druid/index.html`

重点关注:
- SQL执行统计
- 慢SQL记录
- 连接池监控

### 日志监控

查看慢查询日志:
```bash
tail -f logs/jeepay-payment.log | grep "slow"
```

### Redis监控

```bash
# 查看缓存命中率
redis-cli INFO stats | grep keyspace_hits

# 查看内存使用
redis-cli INFO memory | grep used_memory_human

# 查看连接数
redis-cli INFO clients | grep connected_clients
```

---

## 常见问题

### Q1: 索引创建失败

**现象**: 执行SQL脚本时报错 "Duplicate key name"

**解决**: 索引可能已存在，使用以下命令检查:
```sql
SHOW INDEX FROM t_pay_order WHERE Key_name = 'idx_mch_state_created';
```
如果存在，可以跳过该索引创建。

### Q2: 缓存未生效

**现象**: Redis中看不到缓存数据

**排查步骤**:
1. 检查Redis连接配置 `application.yml` 中的 `spring.redis.*` 配置
2. 查看应用日志是否有Redis连接错误
3. 确认 `RedisCacheUtil` Bean是否正确注入

**解决**: 
```bash
# 测试Redis连接
redis-cli -h 127.0.0.1 -p 6379 ping
```

### Q3: 性能提升不明显

**可能原因**:
1. 数据量不足，无法体现优化效果
2. 查询条件未命中索引
3. 缓存未正确使用

**排查方法**:
```sql
-- 检查查询是否使用索引
EXPLAIN SELECT * FROM t_pay_order WHERE ...;

-- 查看表数据量
SELECT COUNT(*) FROM t_pay_order;
```

### Q4: 连接池耗尽

**现象**: 日志中出现 "wait millis xxx, active xxx"

**解决**: 检查是否存在连接泄漏
```sql
-- 查看MySQL连接数
SHOW PROCESSLIST;

-- 查看当前连接详情
SELECT * FROM information_schema.processlist WHERE user = 'jeepay';
```

---

## 回滚指南

如果优化后出现问题，可以按以下步骤回滚:

### 回滚索引
```sql
-- 删除所有新增索引
source /path/to/jeepay/docs/sql/optimization/rollback_indexes.sql
```

### 回滚配置
```bash
# 恢复原配置文件
cp conf/devCommons/config/application.yml.bak conf/devCommons/config/application.yml

# 重启应用
./shutdown.sh && ./startup.sh
```

### 回滚代码
```bash
# 如果使用Git
git revert <commit-hash>

# 或者直接使用备份
cp -r jeepay-service.bak/* jeepay-service/
```

---

## 后续优化建议

1. **持续监控**: 建议运行1-2周后，根据监控数据进一步调优
2. **压力测试**: 使用JMeter等工具进行压力测试，验证系统承载能力
3. **容量规划**: 根据业务增长预测，提前规划分库分表方案
4. **定期维护**: 每月执行一次索引优化和数据清理

---

## 技术支持

如遇到问题，请按以下顺序排查:
1. 查看本文档的"常见问题"部分
2. 查看详细实施文档: `docs/optimization/DATABASE_OPTIMIZATION_IMPLEMENTATION.md`
3. 查看系统日志: `logs/jeepay-*.log`
4. 提交Issue到项目仓库

---

**文档版本**: 1.0  
**最后更新**: 2025-10-15
