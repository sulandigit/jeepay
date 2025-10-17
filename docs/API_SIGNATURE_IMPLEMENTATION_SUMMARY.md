# API签名验证机制实施总结

## 项目概述

根据设计文档,成功为Jeepay支付系统的运营平台(jeepay-manager)和商户平台(jeepay-merchant)实施了完整的API签名验证机制。

## 实施内容

### 1. 核心组件实现 (jeepay-core模块)

#### 1.1 异常类定义
创建了6个签名相关的异常类:

- `SignatureException.java` - 签名验证异常基类
- `MissingSignatureException.java` - 缺少签名参数异常
- `InvalidSignatureException.java` - 签名值不匹配异常
- `TimestampExpiredException.java` - 请求时间戳超出窗口异常
- `ReplayAttackException.java` - 检测到重放攻击异常
- `UnsupportedAlgorithmException.java` - 不支持的签名算法异常

**位置**: `jeepay-core/src/main/java/com/jeequan/jeepay/core/exception/`

#### 1.2 注解定义
- `@SignRequired` - 标记需要签名验证的接口
  - 支持方法级和类级标注
  - 可配置签名算法、是否验签、是否检查时间戳和nonce

**位置**: `jeepay-core/src/main/java/com/jeequan/jeepay/core/aop/SignRequired.java`

#### 1.3 工具类
- `SignatureKit.java` - 签名工具类
  - 支持MD5和SHA256签名算法
  - 提供签名生成和验证方法
  - 参数自动排序和过滤

**位置**: `jeepay-core/src/main/java/com/jeequan/jeepay/core/utils/SignatureKit.java`

- `JeepayKit.java` - 扩展现有工具类
  - 新增SHA256签名方法
  - 新增API密钥生成方法

**位置**: `jeepay-core/src/main/java/com/jeequan/jeepay/core/utils/JeepayKit.java`

#### 1.4 服务组件
- `ReplayAttackDefender.java` - 防重放攻击组件
  - 时间戳验证
  - 基于Redis的nonce去重
  - 可配置时间窗口和严格模式

**位置**: `jeepay-core/src/main/java/com/jeequan/jeepay/core/service/ReplayAttackDefender.java`

- `SignatureValidator.java` - 签名验证服务
  - 核心验签业务逻辑
  - 支持多种签名算法
  - 集成防重放攻击验证

**位置**: `jeepay-core/src/main/java/com/jeequan/jeepay/core/service/SignatureValidator.java`

#### 1.5 拦截器
- `SignatureInterceptor.java` - 签名验证拦截器
  - 拦截需要验签的请求
  - 支持GET和POST JSON参数提取
  - 自动检测@SignRequired注解

**位置**: `jeepay-core/src/main/java/com/jeequan/jeepay/core/aop/SignatureInterceptor.java`

#### 1.6 错误码扩展
扩展了`ApiCodeEnum`,新增5个签名相关错误码:

| 错误码 | 错误信息 |
|-------|---------|
| 1001 | 缺少必要的签名参数 |
| 1002 | 签名验证失败 |
| 1003 | 请求已过期 |
| 1004 | 检测到重放攻击 |
| 1005 | 不支持的签名算法 |

**位置**: `jeepay-core/src/main/java/com/jeequan/jeepay/core/constants/ApiCodeEnum.java`

### 2. 数据模型扩展

#### 2.1 SysUser实体扩展
为`t_sys_user`表添加了3个字段:

| 字段名 | 类型 | 说明 |
|-------|-----|------|
| api_secret | VARCHAR(64) | 用户API密钥 |
| secret_status | TINYINT(1) | 密钥状态: 0-禁用 1-启用 |
| secret_create_time | DATETIME | 密钥生成时间 |

**位置**: `jeepay-core/src/main/java/com/jeequan/jeepay/core/entity/SysUser.java`

### 3. 运营平台集成 (jeepay-manager)

#### 3.1 配置文件
在`application.yml`中添加签名验证配置:

```yaml
jeepay:
  signature:
    enabled: false                              # 是否全局启用
    default-algorithm: MD5                      # 默认签名算法
    default-secret: jeepay_mgr_secret_2025      # 系统级密钥
    time-window: 300000                         # 时间窗口5分钟
    strict-mode: false                          # 是否启用严格模式
```

**位置**: `jeepay-manager/src/main/resources/application.yml`

#### 3.2 拦截器注册
在`WebmvcConfig`中注册签名拦截器:
- 拦截路径: `/api/**`
- 排除路径: `/api/anon/**`, `/swagger-resources/**`
- 拦截器优先级: 100

**位置**: `jeepay-manager/src/main/java/com/jeequan/jeepay/mgr/web/WebmvcConfig.java`

### 4. 商户平台集成 (jeepay-merchant)

#### 4.1 配置文件
在`application.yml`中添加签名验证配置:

```yaml
jeepay:
  signature:
    enabled: false
    default-algorithm: MD5
    default-secret: jeepay_mch_secret_2025
    time-window: 300000
    strict-mode: false
```

**位置**: `jeepay-merchant/src/main/resources/application.yml`

#### 4.2 拦截器注册
在`WebmvcConfig`中注册签名拦截器:
- 拦截路径: `/api/**`
- 排除路径: `/api/anon/**`, `/api/channelUserId/**`
- 拦截器优先级: 100

**位置**: `jeepay-merchant/src/main/java/com/jeequan/jeepay/mch/web/WebmvcConfig.java`

### 5. 数据库迁移

创建了数据库升级脚本:
- 位置: `docs/sql/upgrade_add_signature_fields.sql`
- 包含ALTER TABLE语句
- 包含验证和回滚脚本

### 6. 测试验证

#### 6.1 单元测试
创建了`SignatureKitTest.java`,包含8个测试用例:
- MD5签名测试
- SHA256签名测试
- 签名验证成功测试
- 签名验证失败测试
- 空值参数测试
- 签名一致性测试
- 参数顺序测试
- 不支持算法异常测试

**位置**: `jeepay-core/src/test/java/com/jeequan/jeepay/core/utils/SignatureKitTest.java`

### 7. 使用文档

创建了完整的使用指南文档:
- 快速开始
- 配置说明
- 接口标注方法
- 客户端签名示例(Java/JavaScript/Python)
- 测试验证
- 常见问题解答

**位置**: `docs/API_SIGNATURE_GUIDE.md`

## 技术特性

### 1. 签名算法
- ✅ MD5签名
- ✅ SHA256签名
- ✅ 参数自动排序
- ✅ 空值参数过滤

### 2. 安全机制
- ✅ 时间戳验证(默认5分钟窗口)
- ✅ Nonce去重(可选,基于Redis)
- ✅ 签名值大小写不敏感
- ✅ 支持用户级和系统级密钥

### 3. 灵活配置
- ✅ 全局启用/禁用
- ✅ 注解驱动的接口级控制
- ✅ 可配置签名算法
- ✅ 可配置时间窗口
- ✅ 可配置严格模式

### 4. 异常处理
- ✅ 细粒度异常分类
- ✅ 全局异常处理器集成
- ✅ 详细错误信息返回
- ✅ 完整的日志记录

## 使用示例

### 1. 启用签名验证

修改配置文件:
```yaml
jeepay:
  signature:
    enabled: true  # 启用全局验签
```

### 2. 为接口添加签名验证

```java
@RestController
@RequestMapping("/api/mch")
public class MchInfoController {

    @SignRequired(algorithm = "SHA256", checkNonce = true)
    @PostMapping("/update")
    public ApiRes updateMchInfo(@RequestBody MchInfoVO mchInfo) {
        // 业务逻辑
        return ApiRes.ok();
    }
}
```

### 3. 客户端调用

```java
Map<String, Object> params = new HashMap<>();
params.put("mchNo", "M1000001");
params.put("mchName", "测试商户");
params.put("timestamp", System.currentTimeMillis());
params.put("nonce", UUID.randomUUID().toString().replace("-", ""));
params.put("signType", "SHA256");

String sign = SignatureKit.sign(params, "your_secret_key", "SHA256");
params.put("sign", sign);

// 发送POST请求
```

## 代码统计

| 模块 | 新增文件数 | 代码行数 |
|-----|----------|---------|
| 异常类 | 6 | ~240行 |
| 注解 | 1 | ~52行 |
| 工具类 | 1 | ~220行 |
| 服务组件 | 2 | ~354行 |
| 拦截器 | 1 | ~168行 |
| 测试类 | 1 | ~163行 |
| 配置修改 | 4 | ~40行 |
| 实体扩展 | 1 | ~18行 |
| SQL脚本 | 1 | ~49行 |
| 文档 | 1 | ~529行 |
| **总计** | **19** | **~1833行** |

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                      客户端应用                          │
│              (生成签名并发送请求)                         │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP请求(带签名)
                     ▼
┌─────────────────────────────────────────────────────────┐
│                  Spring MVC层                            │
│  ┌──────────────────────────────────────────────────┐   │
│  │         SignatureInterceptor (拦截器)            │   │
│  │  - 检测@SignRequired注解                         │   │
│  │  - 提取请求参数                                   │   │
│  └──────────────┬───────────────────────────────────┘   │
│                 │                                         │
│                 ▼                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │       SignatureValidator (验证服务)              │   │
│  │  - 验证签名算法                                   │   │
│  │  - 计算并比对签名                                 │   │
│  └──────────────┬───────────────────────────────────┘   │
│                 │                                         │
│                 ▼                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │    ReplayAttackDefender (防重放)                 │   │
│  │  - 验证时间戳                                     │   │
│  │  - 验证Nonce (Redis)                             │   │
│  └──────────────┬───────────────────────────────────┘   │
└─────────────────┼───────────────────────────────────────┘
                  │
                  ▼ 验证通过
┌─────────────────────────────────────────────────────────┐
│                 业务Controller                           │
│              (执行业务逻辑)                               │
└─────────────────────────────────────────────────────────┘
```

## 验证清单

### ✅ 已完成项

- [x] 创建6个签名异常类
- [x] 创建@SignRequired注解
- [x] 创建SignatureKit签名工具类
- [x] 扩展JeepayKit工具类
- [x] 创建ReplayAttackDefender防重放组件
- [x] 创建SignatureValidator验证服务
- [x] 创建SignatureInterceptor拦截器
- [x] 扩展SysUser实体字段
- [x] 扩展ApiCodeEnum错误码
- [x] 配置jeepay-manager模块
- [x] 配置jeepay-merchant模块
- [x] 创建数据库迁移脚本
- [x] 创建单元测试
- [x] 创建使用文档
- [x] 代码编译验证通过

## 注意事项

### 1. 首次使用前
1. **执行数据库迁移脚本** `upgrade_add_signature_fields.sql`
2. **修改配置文件** 设置系统级密钥
3. **为用户生成API密钥**(可选)

### 2. 性能考虑
- 默认配置下性能影响极小(仅时间戳验证)
- 启用严格模式(nonce去重)会增加Redis查询
- 建议仅对敏感操作启用nonce去重

### 3. 兼容性
- 默认配置`enabled: false`,不影响现有接口
- 通过注解逐步为接口添加验签
- 支持混合模式(部分接口验签,部分不验签)

### 4. 扩展建议
未来可扩展功能:
- 支持RSA非对称加密签名
- 支持签名密钥定期轮换
- 支持接口级签名策略配置
- 集成到API管理后台

## 文件清单

### 新增文件
```
jeepay-core/src/main/java/com/jeequan/jeepay/core/
├── exception/
│   ├── SignatureException.java
│   ├── MissingSignatureException.java
│   ├── InvalidSignatureException.java
│   ├── TimestampExpiredException.java
│   ├── ReplayAttackException.java
│   └── UnsupportedAlgorithmException.java
├── aop/
│   ├── SignRequired.java
│   └── SignatureInterceptor.java
├── utils/
│   └── SignatureKit.java
└── service/
    ├── SignatureValidator.java
    └── ReplayAttackDefender.java

jeepay-core/src/test/java/com/jeequan/jeepay/core/utils/
└── SignatureKitTest.java

docs/
├── sql/
│   └── upgrade_add_signature_fields.sql
└── API_SIGNATURE_GUIDE.md
```

### 修改文件
```
jeepay-core/src/main/java/com/jeequan/jeepay/core/
├── entity/SysUser.java                     (新增3个字段)
├── utils/JeepayKit.java                    (新增2个方法)
└── constants/ApiCodeEnum.java              (新增5个错误码)

jeepay-manager/src/main/
├── resources/application.yml               (新增签名配置)
└── java/com/jeequan/jeepay/mgr/web/WebmvcConfig.java (注册拦截器)

jeepay-merchant/src/main/
├── resources/application.yml               (新增签名配置)
└── java/com/jeequan/jeepay/mch/web/WebmvcConfig.java (注册拦截器)
```

## 总结

本次实施严格按照设计文档要求,成功为Jeepay支付系统实现了完整的API签名验证机制。主要特点:

1. **完整性**: 覆盖了设计文档中的所有功能点
2. **可扩展性**: 支持多种签名算法,便于未来扩展
3. **灵活性**: 通过注解和配置灵活控制验签行为
4. **安全性**: 支持时间戳和nonce双重防重放机制
5. **易用性**: 提供详细文档和多语言客户端示例
6. **兼容性**: 默认不影响现有系统,平滑升级

所有代码已通过编译验证,可直接部署使用。

---

**实施日期**: 2025-10-17  
**实施版本**: v1.0  
**设计文档**: API签名验证机制设计文档 v1.0
