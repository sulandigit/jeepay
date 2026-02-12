# Nacos服务注册与配置中心部署指南

## 1. 概述

Jeepay 2.3.0版本引入了Nacos作为服务注册中心和配置中心,实现微服务的动态发现和集中配置管理。

## 2. Nacos安装部署

### 2.1 单机模式部署

#### 下载Nacos

```bash
# 下载Nacos 2.3.2
wget https://github.com/alibaba/nacos/releases/download/2.3.2/nacos-server-2.3.2.tar.gz

# 解压
tar -xzf nacos-server-2.3.2.tar.gz
cd nacos
```

#### 启动Nacos

```bash
# Linux/Unix/Mac
sh bin/startup.sh -m standalone

# Windows
startup.cmd -m standalone
```

访问控制台: http://localhost:8848/nacos
默认账号密码: nacos/nacos

### 2.2 集群模式部署(生产推荐)

#### 1. 配置数据库

创建MySQL数据库并导入nacos配置表:

```sql
CREATE DATABASE nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

执行 `conf/mysql-schema.sql` 初始化表结构。

#### 2. 修改配置文件

编辑 `conf/application.properties`:

```properties
# 数据库配置
spring.datasource.platform=mysql
db.num=1
db.url.0=jdbc:mysql://127.0.0.1:3306/nacos_config?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai
db.user.0=root
db.password.0=yourpassword
```

#### 3. 配置集群节点

编辑 `conf/cluster.conf`:

```
192.168.1.101:8848
192.168.1.102:8848
192.168.1.103:8848
```

#### 4. 启动集群

在每个节点上执行:

```bash
sh bin/startup.sh
```

### 2.3 Docker部署

```bash
docker run --name nacos -d \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  -e PREFER_HOST_MODE=hostname \
  nacos/nacos-server:v2.3.2
```

## 3. Nacos配置管理

### 3.1 创建命名空间

1. 登录Nacos控制台
2. 进入 `命名空间` 菜单
3. 创建命名空间: `jeepay`
4. 记录命名空间ID

### 3.2 配置共享配置文件

在jeepay命名空间下创建以下共享配置:

#### application-common.yml

```yaml
# 公共配置
logging:
  level:
    root: INFO
    com.jeequan: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      show-details: always
```

#### application-datasource.yml

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/jeepaydb?useUnicode=true&characterEncoding=utf8&autoReconnect=true&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: yourpassword
```

#### application-redis.yml

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    timeout: 6000ms
    lettuce:
      pool:
        max-active: 100
        max-wait: -1ms
        max-idle: 16
        min-idle: 8
```

## 4. Jeepay服务配置

### 4.1 环境变量配置

```bash
# Nacos服务器地址
export NACOS_ADDR=127.0.0.1:8848

# Nacos命名空间ID
export NACOS_NAMESPACE=jeepay

# 运行环境
export SPRING_PROFILES_ACTIVE=dev
```

### 4.2 服务启动参数

```bash
java -jar jeepay-manager.jar \
  --spring.cloud.nacos.discovery.server-addr=${NACOS_ADDR} \
  --spring.cloud.nacos.discovery.namespace=${NACOS_NAMESPACE}
```

## 5. 服务注册验证

启动Jeepay服务后,在Nacos控制台验证:

1. 进入 `服务管理` -> `服务列表`
2. 选择命名空间 `jeepay`
3. 查看注册的服务:
   - jeepay-manager (端口: 9217)
   - jeepay-merchant (端口: 9218)
   - jeepay-payment (端口: 9216)

## 6. 健康检查配置

服务健康检查路径: `/actuator/health`

健康检查间隔:
- 心跳间隔: 5秒
- 心跳超时: 15秒
- IP删除超时: 30秒

## 7. 故障排查

### 7.1 服务注册失败

检查项:
- Nacos服务是否正常运行
- 网络连接是否正常
- 命名空间配置是否正确
- 服务端口是否被占用

### 7.2 配置拉取失败

检查项:
- 配置文件是否已创建
- Data ID和Group是否匹配
- 配置内容格式是否正确

### 7.3 日志查看

```bash
# Nacos日志目录
tail -f nacos/logs/nacos.log

# 服务日志
tail -f logs/jeepay-manager.log
```

## 8. 最佳实践

1. **环境隔离**: 为dev、test、prod创建不同的命名空间
2. **配置版本**: 使用配置版本管理,支持回滚
3. **安全加固**: 
   - 修改默认账号密码
   - 启用鉴权功能
   - 配置白名单
4. **监控告警**: 集成监控系统,监控服务健康状态
5. **定期备份**: 定期备份Nacos配置数据

## 9. 参考资料

- [Nacos官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Spring Cloud Alibaba文档](https://sca.aliyun.com/)
