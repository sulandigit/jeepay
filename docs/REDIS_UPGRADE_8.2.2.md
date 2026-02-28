# Redis 升级到 8.2.2 版本指南

## 升级概述

本次升级将Jeepay系统中的Redis从 `redis:latest` 升级到最新稳定版本 `redis:8.2.2-alpine`，提供更好的性能、安全性和稳定性。

## 升级版本信息

- **原版本**: redis:latest (未指定具体版本)
- **目标版本**: redis:8.2.2-alpine
- **发布日期**: 2024年10月3日
- **升级类型**: 主要版本升级

## Redis 8.2.2 新特性

### 主要改进
1. **性能提升**: 相比8.0版本有超过30项性能改进，某些命令性能提升高达87%
2. **安全修复**: 修复了多个安全漏洞（CVE-2025-49844, CVE-2025-46817等）
3. **新功能**: 
   - 新增Vector Set数据结构（AI用例支持）
   - Streams新命令：XDELEX和XACKDEL
   - BITMAP新操作符：DIFF, DIFF1, ANDOR, ONE

### 兼容性
- 与Spring Boot 2.4.8完全兼容
- Lettuce客户端支持良好
- 向后兼容现有Redis命令

## 升级变更详情

### 1. Docker配置更新

#### docker-compose.yml
```yaml
# 原配置
redis:
  hostname: redis
  container_name: jeepay-redis
  image: redis:latest

# 更新后配置
redis:
  hostname: redis
  container_name: jeepay-redis
  image: redis:8.2.2-alpine
  ports:
    - "6380:6379"
  networks:
    jeepay:
      ipv4_address: 172.20.0.12
  volumes:
    - redis:/data
  # Redis配置优化（可选）
  command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
```

**配置说明**:
- 使用alpine版本减小镜像大小
- 启用AOF持久化（--appendonly yes）
- 设置内存限制512MB，使用LRU淘汰策略

### 2. 应用配置更新

#### 主机名配置修正
将所有应用配置文件中的Redis主机名从 `redis6` 统一更新为 `redis`：

**conf/payment/application.yml**:
```yaml
spring:
  redis:
    host: redis  # 原：redis6
    port: 6379
    database: 3
```

**conf/manager/application.yml**:
```yaml
spring:
  redis:
    host: redis  # 原：redis6
    port: 6379
    database: 1
```

**conf/merchant/application.yml**:
```yaml
spring:
  redis:
    host: redis  # 原：redis6
    port: 6379
    database: 2
```

## 升级步骤

### 1. 备份数据（重要）
```bash
# 备份Redis数据
docker exec jeepay-redis redis-cli BGSAVE

# 导出数据（可选）
docker exec jeepay-redis redis-cli --rdb /data/backup.rdb
```

### 2. 停止现有服务
```bash
docker-compose down
```

### 3. 拉取新镜像
```bash
docker pull redis:8.2.2-alpine
```

### 4. 启动升级后的服务
```bash
docker-compose up -d
```

### 5. 验证升级
```bash
# 检查Redis版本
docker exec jeepay-redis redis-cli INFO server

# 检查连接状态
docker exec jeepay-redis redis-cli ping

# 检查数据库连接
docker logs jeepay-payment | grep -i redis
docker logs jeepay-manager | grep -i redis  
docker logs jeepay-merchant | grep -i redis
```

## 回滚方案

如果升级后出现问题，可以按以下步骤回滚：

1. **停止服务**:
   ```bash
   docker-compose down
   ```

2. **恢复配置**:
   - 将docker-compose.yml中的镜像改回 `redis:latest`
   - 恢复应用配置文件中的主机名设置

3. **重启服务**:
   ```bash
   docker-compose up -d
   ```

## 监控和测试

### 性能监控
- 监控Redis内存使用情况
- 关注响应时间变化
- 检查连接池状态

### 功能测试
1. **支付功能测试**
   - 创建支付订单
   - 验证订单状态缓存
   - 测试支付回调处理

2. **商户管理测试**
   - 商户登录认证
   - 配置信息缓存
   - 权限验证

3. **系统配置测试**
   - 系统配置读取
   - 配置更新广播
   - 缓存刷新机制

## 注意事项

1. **数据安全**: 升级前务必备份Redis数据
2. **服务依赖**: 确保所有依赖Redis的服务都正常启动
3. **内存配置**: 新版本默认启用了内存优化，可根据实际情况调整
4. **持久化**: 已启用AOF持久化，确保数据安全
5. **监控**: 升级后密切监控系统性能和错误日志

## 故障排除

### 常见问题

1. **连接失败**
   - 检查网络配置
   - 验证主机名解析
   - 确认端口映射

2. **内存不足**
   - 调整maxmemory设置
   - 优化内存使用策略
   - 清理无用缓存

3. **数据丢失**
   - 检查持久化配置
   - 恢复备份数据
   - 验证数据完整性

### 联系信息
如遇问题，请联系系统管理员或查看项目文档。

---

**升级完成日期**: 2024年10月10日  
**文档版本**: 1.0  
**维护人员**: 系统管理员