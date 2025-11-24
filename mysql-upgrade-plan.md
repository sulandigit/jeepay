# Jeepay 支付系统 MySQL 版本升级实施计划

## 当前环境分析

### 现有配置总结

| 组件 | 当前版本 | 目标版本 | 位置 |
|------|----------|----------|------|
| MySQL Server | mysql:8 (Docker) | mysql:8.0.35 | docker-compose.yml |
| MySQL Connector/J | 8.0.28 | 8.0.35 | pom.xml |
| 代码生成器驱动 | 8.0.28 | 8.0.35 | jeepay-z-codegen/pom.xml |

### 受影响的配置文件

1. **Docker 部署配置**
   - `/data/workspace/jeepay/docker-compose.yml` - MySQL 镜像版本
   
2. **Maven 依赖配置**
   - `/data/workspace/jeepay/pom.xml` - MySQL 连接器版本（全局）
   - `/data/workspace/jeepay/jeepay-z-codegen/pom.xml` - 代码生成器专用驱动
   
3. **应用配置文件**
   - `/data/workspace/jeepay/conf/devCommons/config/application.yml` - 开发环境通用配置
   - `/data/workspace/jeepay/conf/manager/application.yml` - 运营平台配置
   - `/data/workspace/jeepay/conf/merchant/application.yml` - 商户平台配置
   - `/data/workspace/jeepay/conf/payment/application.yml` - 支付网关配置

4. **MySQL 配置文件**
   - `/data/workspace/jeepay/docs/install/include/my.cnf` - MySQL 服务器配置

## 详细实施计划

### 阶段一：前置准备（预计时间：30分钟）

#### 1.1 环境评估检查清单

```bash
# 检查当前 MySQL 版本
docker exec jeepay-mysql mysql --version

# 检查数据库大小
docker exec jeepay-mysql mysql -u root -p -e "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 1) AS 'DB Size in MB' FROM information_schema.tables WHERE table_schema='jeepaydb';"

# 检查表完整性
docker exec jeepay-mysql mysql -u root -p -e "USE jeepaydb; CHECK TABLE t_pay_order, t_mch_info, t_pay_interface_define;"

# 检查当前连接数
docker exec jeepay-mysql mysql -u root -p -e "SHOW STATUS LIKE 'Threads_connected';"
```

#### 1.2 数据备份策略

```bash
# 1. 全量数据备份
docker exec jeepay-mysql mysqldump -u root -p --single-transaction --routines --triggers jeepaydb > jeepaydb_backup_$(date +%Y%m%d_%H%M%S).sql

# 2. 配置文件备份
mkdir -p ./backup/config/$(date +%Y%m%d_%H%M%S)
cp docker-compose.yml ./backup/config/$(date +%Y%m%d_%H%M%S)/
cp pom.xml ./backup/config/$(date +%Y%m%d_%H%M%S)/
cp -r conf/ ./backup/config/$(date +%Y%m%d_%H%M%S)/

# 3. Docker 数据卷备份
docker run --rm -v jeepay_mysql:/data -v $(pwd):/backup ubuntu tar czf /backup/mysql_volume_backup_$(date +%Y%m%d_%H%M%S).tar.gz /data
```

#### 1.3 应用服务状态检查

```bash
# 检查所有 Jeepay 相关容器状态
docker ps -a | grep jeepay

# 检查应用日志
docker logs jeepay-payment --tail 50
docker logs jeepay-manager --tail 50
docker logs jeepay-merchant --tail 50
```

### 阶段二：升级执行（预计时间：45分钟）

#### 2.1 停止应用服务（5分钟）

```bash
# 按依赖顺序停止服务
docker stop jeepay-ui-payment jeepay-ui-manager jeepay-ui-merchant
docker stop jeepay-payment jeepay-manager jeepay-merchant
# 保持 MySQL、Redis、ActiveMQ 运行
```

#### 2.2 更新 Maven 依赖配置（10分钟）

**主 pom.xml 更新：**
- 将 `<mysql.version>8.0.28</mysql.version>` 更新为 `<mysql.version>8.0.35</mysql.version>`

**代码生成器 pom.xml 更新：**
- 将 `mysql-connector-java` 版本从 8.0.28 更新为 8.0.35

#### 2.3 MySQL 服务器升级（15分钟）

```bash
# 1. 停止当前 MySQL 容器
docker stop jeepay-mysql

# 2. 更新 docker-compose.yml 中的镜像版本
# 将 image: mysql:8 更改为 image: mysql:8.0.35

# 3. 拉取新镜像
docker pull mysql:8.0.35

# 4. 启动新版本 MySQL
docker-compose up -d mysql

# 5. 等待启动完成
sleep 30

# 6. 检查 MySQL 升级状态
docker exec jeepay-mysql mysql -u root -p -e "SELECT VERSION();"
```

#### 2.4 优化 MySQL 配置（10分钟）

**更新 my.cnf 配置以利用 8.0.35 新特性：**

```ini
# 性能优化配置
innodb_buffer_pool_size = 256M  # 根据可用内存调整
innodb_log_buffer_size = 16M
innodb_io_capacity = 2000
innodb_flush_log_at_trx_commit = 2  # 提升性能，略降安全性

# 连接管理优化
max_connections = 1000
max_connect_errors = 10

# 新版本特性
default_authentication_plugin = caching_sha2_password
validate_password.policy = MEDIUM
```

#### 2.5 重新编译项目（5分钟）

```bash
# 清理并重新编译
mvn clean install -DskipTests

# 或使用 Docker 方式重新构建
docker-compose build --no-cache payment manager merchant
```

### 阶段三：验证测试（预计时间：60分钟）

#### 3.1 数据库连接验证（10分钟）

```bash
# 1. 启动应用服务
docker-compose up -d payment manager merchant

# 2. 检查连接日志
docker logs jeepay-payment | grep -i "mysql\|database\|connection"
docker logs jeepay-manager | grep -i "mysql\|database\|connection"
docker logs jeepay-merchant | grep -i "mysql\|database\|connection"

# 3. 验证数据库连接池状态
# 可通过 Druid 监控页面查看：http://localhost:9217/druid/
```

#### 3.2 功能验证测试（30分钟）

**基础功能验证清单：**

1. **数据库连接测试**
   ```bash
   # 测试应用能否正常连接数据库
   curl -X GET http://localhost:9217/api/sysconfigs
   ```

2. **CRUD 操作验证**
   - 用户登录功能测试
   - 商户信息查询测试
   - 支付订单创建测试
   - 数据更新操作测试

3. **事务完整性验证**
   - 支付流程事务测试
   - 回滚机制验证

#### 3.3 性能对比测试（20分钟）

**关键性能指标监控：**

```bash
# 1. 查询性能测试
time docker exec jeepay-mysql mysql -u root -p -e "SELECT COUNT(*) FROM jeepaydb.t_pay_order;"

# 2. 连接池性能监控
# 通过 Druid 监控查看：
# - 活跃连接数
# - 平均响应时间
# - SQL 执行次数

# 3. 数据库性能指标
docker exec jeepay-mysql mysql -u root -p -e "SHOW STATUS LIKE 'Questions'; SHOW STATUS LIKE 'Uptime';"
```

### 阶段四：上线部署（预计时间：15分钟）

#### 4.1 启动完整服务

```bash
# 启动所有服务
docker-compose up -d

# 检查所有容器状态
docker ps -a | grep jeepay

# 验证服务可访问性
curl -I http://localhost:9226  # 支付前端
curl -I http://localhost:9227  # 管理前端  
curl -I http://localhost:9228  # 商户前端
```

#### 4.2 监控设置

**关键监控指标：**

1. **数据库性能监控**
   - QPS/TPS 监控
   - 查询响应时间
   - 连接池使用率
   - 内存使用情况

2. **应用服务监控**
   - HTTP 响应时间
   - 错误率监控
   - 业务功能可用性

3. **告警配置**
   - 数据库连接失败率 > 1%
   - 查询响应时间 > 1000ms
   - 连接池使用率 > 80%

## 回滚策略

### 快速回滚流程（预计时间：20分钟）

#### 回滚触发条件
- 数据库连接失败率 > 1%
- 关键业务功能异常
- 性能下降超过 20%
- 数据不一致

#### 回滚执行步骤

```bash
# 1. 立即停止所有应用服务
docker-compose down

# 2. 恢复原 MySQL 镜像版本
# 修改 docker-compose.yml: image: mysql:8.0.25
docker-compose up -d mysql

# 3. 从备份恢复数据（如有必要）
docker exec jeepay-mysql mysql -u root -p jeepaydb < jeepaydb_backup_YYYYMMDD_HHMMSS.sql

# 4. 恢复原配置文件
cp ./backup/config/YYYYMMDD_HHMMSS/pom.xml ./
cp ./backup/config/YYYYMMDD_HHMMSS/docker-compose.yml ./

# 5. 重新编译和启动服务
mvn clean install -DskipTests
docker-compose up -d
```

## 风险控制措施

### 高风险操作管控
1. **数据备份验证** - 每次备份后验证数据完整性
2. **分阶段执行** - 避免一次性大规模变更
3. **监控告警** - 实时监控关键指标
4. **快速回滚** - 准备快速回滚方案

### 业务连续性保障
1. **最小化停机时间** - 仅在必要时停止服务
2. **蓝绿部署考虑** - 生产环境建议采用蓝绿部署
3. **数据一致性检查** - 升级前后数据对比验证

## 成功标准

### 升级成功验证检查项

- [ ] MySQL 版本显示为 8.0.35
- [ ] 所有应用服务正常启动
- [ ] 数据库连接无错误日志
- [ ] 关键业务功能正常
- [ ] 性能指标无明显下降
- [ ] 数据完整性验证通过
- [ ] 监控告警正常

### 性能基线对比

| 指标 | 升级前基线 | 升级后目标 | 验收标准 |
|------|------------|------------|----------|
| 数据库连接时间 | < 100ms | < 100ms | 不降低 |
| 查询平均响应时间 | < 500ms | < 400ms | 优化目标 |
| 支付订单创建 | < 1s | < 1s | 保持稳定 |
| 并发连接数 | 支持100+ | 支持150+ | 提升目标 |

## 联系信息

**技术负责人：** [填写负责人信息]
**执行时间：** [填写计划执行时间]
**应急联系：** [填写应急联系方式]

---

**注意事项：**
1. 生产环境执行前必须在测试环境完整验证
2. 建议在业务低峰期执行升级
3. 准备回滚方案并验证回滚流程可行性
4. 升级过程中保持相关人员在线，随时准备处理突发情况