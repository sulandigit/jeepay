# Jeepay JDK 17 升级指导文档

## 概述

本文档提供Jeepay支付系统从JDK 8升级到JDK 17的详细指导，包括环境准备、配置更新、代码修改和部署验证等各个环节。

## 升级前准备

### 1. 环境要求

| 组件 | JDK 8 版本 | JDK 17 版本 | 备注 |
|------|-----------|------------|------|
| JDK | 1.8.x | 17.x (LTS) | 推荐使用 Eclipse Temurin |
| Maven | 3.6+ | 3.8+ | 支持JDK 17编译 |
| Spring Boot | 2.4.8 | 2.7.18 | 已升级以支持JDK 17 |
| FastJSON | 1.2.83 | 2.0.43 | API有变化，需要修改import |

### 2. 备份清单

升级前请备份以下内容：
- [ ] 完整代码仓库
- [ ] 数据库数据
- [ ] 配置文件
- [ ] 运行时日志
- [ ] Docker镜像

## 升级步骤

### 阶段1：Maven配置升级

#### 1.1 更新根POM.xml

已完成的配置更新：

```xml
<!-- Java版本升级 -->
<java.version>17</java.version>

<!-- Spring Boot版本升级 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<!-- 依赖版本升级 -->
<mybatis.plus.starter.version>3.5.3</mybatis.plus.starter.version>
<spring.security.version>5.7.5</spring.security.version>
<fastjson.version>2.0.43</fastjson.version>
```

#### 1.2 编译器配置

添加了JDK 17编译器配置：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>17</source>
        <target>17</target>
        <compilerArgs>
            <arg>--add-opens=java.base/java.lang=ALL-UNNAMED</arg>
            <arg>--add-opens=java.base/java.util=ALL-UNNAMED</arg>
            <arg>--add-opens=java.base/java.time=ALL-UNNAMED</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

### 阶段2：Docker配置升级

#### 2.1 基础镜像更新

所有Dockerfile已更新为JDK 17镜像：

```dockerfile
# 旧版本
FROM openjdk:8u342-jdk

# 新版本
FROM eclipse-temurin:17-jre
```

#### 2.2 JVM参数优化

新的JVM启动参数：

```bash
java -Xms512m -Xmx2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     --add-opens java.base/java.lang=ALL-UNNAMED \
     --add-opens java.base/java.util=ALL-UNNAMED \
     -jar application.jar
```

### 阶段3：代码兼容性修复

#### 3.1 FastJSON升级

所有FastJSON引用已从1.x升级到2.x：

```java
// 旧版本import
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

// 新版本import
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
```

影响的文件：
- `jeepay-core` 模块：9个文件
- `jeepay-components-mq` 模块：6个文件

## 编译和测试

### 1. 本地编译测试

```bash
# 安装JDK 17
# 下载并安装 Eclipse Temurin 17

# 设置环境变量
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# 验证Java版本
java -version

# 编译项目
mvn clean compile -DskipTests

# 运行测试
mvn test

# 打包项目
mvn clean package -DskipTests
```

### 2. Docker编译测试

```bash
# 编译Docker镜像
docker build -t jeepay-test:jdk17 .

# 测试特定模块
docker build -t jeepay-payment:jdk17 --build-arg PLATFORM=payment .
docker build -t jeepay-manager:jdk17 --build-arg PLATFORM=manager .
docker build -t jeepay-merchant:jdk17 --build-arg PLATFORM=merchant .
```

## 部署方案

### 1. 蓝绿部署策略

```mermaid
graph LR
    A[当前JDK8环境] --> B[新建JDK17环境]
    B --> C[部署JDK17应用]
    C --> D[健康检查]
    D --> E[流量切换]
    E --> F[下线JDK8环境]
```

### 2. 分模块部署

| 部署顺序 | 模块 | 风险等级 | 回滚时间 |
|----------|------|----------|----------|
| 1 | jeepay-core | 低 | 5分钟 |
| 2 | jeepay-service | 低 | 5分钟 |
| 3 | jeepay-manager | 中 | 10分钟 |
| 4 | jeepay-merchant | 中 | 10分钟 |
| 5 | jeepay-payment | 高 | 15分钟 |

### 3. Docker Compose部署

```bash
# 使用新的docker-compose.yml
docker-compose down
docker-compose build --no-cache
docker-compose up -d

# 检查服务状态
docker-compose ps
docker-compose logs payment
docker-compose logs manager
docker-compose logs merchant
```

## 监控和验证

### 1. 关键指标监控

升级后需要重点监控的指标：

| 监控项目 | 正常范围 | 告警阈值 | 处理建议 |
|----------|----------|----------|----------|
| CPU使用率 | < 70% | > 80% | 检查G1GC配置 |
| 内存使用率 | < 80% | > 85% | 调整堆内存大小 |
| GC暂停时间 | < 200ms | > 500ms | 优化GC参数 |
| 应用启动时间 | < 60s | > 120s | 检查依赖加载 |
| API响应时间 | < 500ms | > 2000ms | 性能排查 |

### 2. 功能验证清单

- [ ] 用户登录功能
- [ ] 支付订单创建
- [ ] 支付渠道调用
- [ ] 消息队列收发
- [ ] 文件上传下载
- [ ] 数据库操作
- [ ] 缓存读写
- [ ] 第三方接口调用

### 3. 健康检查

```bash
# 检查应用状态
curl http://localhost:9216/actuator/health
curl http://localhost:9217/actuator/health
curl http://localhost:9218/actuator/health

# 检查JVM信息
curl http://localhost:9216/actuator/info
```

## 故障排查

### 1. 常见问题

#### 问题1：模块访问异常
```
IllegalAccessError: class X cannot access class Y
```

**解决方案：**
添加JVM参数：
```bash
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
```

#### 问题2：FastJSON兼容问题
```
ClassNotFoundException: com.alibaba.fastjson.JSON
```

**解决方案：**
确认所有import已更新为fastjson2包名

#### 问题3：Spring Boot启动失败
```
Unsupported class file major version
```

**解决方案：**
检查所有依赖的JDK版本兼容性

### 2. 性能调优

#### JVM参数调优建议

```bash
# 生产环境推荐配置
-Xms2g -Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UnlockExperimentalVMOptions
-XX:+UseStringDeduplication
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
```

## 回滚方案

### 快速回滚步骤

1. **立即回滚（5分钟内）**
   ```bash
   # 停止JDK 17服务
   docker-compose down
   
   # 切换到备份的JDK 8配置
   git checkout backup-jdk8-branch
   
   # 启动JDK 8服务
   docker-compose up -d
   ```

2. **数据处理**
   - 检查数据一致性
   - 必要时恢复数据库备份
   - 清理不兼容的缓存数据

3. **验证回滚**
   - 检查所有服务状态
   - 验证核心功能
   - 确认监控指标正常

## 风险控制

### 1. 升级风险等级

| 风险类型 | 发生概率 | 影响程度 | 缓解措施 |
|----------|----------|----------|----------|
| 依赖兼容性问题 | 中 | 高 | 预先测试验证 |
| 性能退化 | 低 | 中 | 性能基准对比 |
| 功能异常 | 低 | 高 | 全面回归测试 |
| 部署失败 | 低 | 高 | 蓝绿部署策略 |

### 2. 应急联系人

| 角色 | 姓名 | 联系方式 | 职责 |
|------|------|----------|------|
| 技术负责人 | - | - | 技术决策 |
| 运维负责人 | - | - | 部署运维 |
| 业务负责人 | - | - | 业务验证 |

## 总结

本次JDK 17升级涉及以下主要变更：

1. **Maven配置**：Java版本、Spring Boot版本、依赖版本升级
2. **Docker配置**：基础镜像、JVM参数优化
3. **代码修改**：FastJSON 2.x兼容性修复
4. **部署配置**：JVM参数和模块访问配置

升级完成后，系统将获得：
- 更好的性能表现（预期提升10-15%）
- 更强的安全性
- 长期技术支持
- 现代化的Java特性支持

请按照本文档步骤进行升级，并在每个阶段进行充分的测试验证。如遇问题，请及时联系技术团队。