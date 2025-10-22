# API签名验证机制 - 使用指南

## 概述

本文档介绍Jeepay API签名验证机制的使用方法,包括如何为接口添加签名验证、客户端如何生成签名等。

## 目录

1. [快速开始](#快速开始)
2. [配置说明](#配置说明)
3. [接口标注](#接口标注)
4. [客户端签名](#客户端签名)
5. [测试验证](#测试验证)
6. [常见问题](#常见问题)

---

## 快速开始

### 1. 数据库升级

执行数据库迁移脚本:

```sql
-- 位置: docs/sql/upgrade_add_signature_fields.sql
USE jeepay;

ALTER TABLE t_sys_user
ADD COLUMN api_secret VARCHAR(64) DEFAULT NULL COMMENT '用户API密钥,用于签名验证',
ADD COLUMN secret_status TINYINT(1) DEFAULT 1 COMMENT '密钥状态: 0-禁用 1-启用',
ADD COLUMN secret_create_time DATETIME DEFAULT NULL COMMENT '密钥生成时间';
```

### 2. 启用签名验证

在`application.yml`中配置:

**运营平台(jeepay-manager)**:
```yaml
jeepay:
  signature:
    enabled: true                              # 全局启用签名验证
    default-algorithm: MD5                     # 默认签名算法
    default-secret: jeepay_mgr_secret_2025     # 系统级默认密钥
    time-window: 300000                        # 时间窗口5分钟
    strict-mode: false                         # 是否启用nonce去重
```

**商户平台(jeepay-merchant)**:
```yaml
jeepay:
  signature:
    enabled: true
    default-algorithm: MD5
    default-secret: jeepay_mch_secret_2025
    time-window: 300000
    strict-mode: false
```

### 3. 为接口添加签名验证

在需要验签的Controller方法或类上添加`@SignRequired`注解:

```java
@RestController
@RequestMapping("/api/mch")
public class MchInfoController {

    /**
     * 商户信息修改接口 - 启用SHA256签名和nonce防重放
     */
    @SignRequired(algorithm = "SHA256", checkNonce = true)
    @PostMapping("/update")
    public ApiRes updateMchInfo(@RequestBody MchInfoVO mchInfo) {
        // 业务逻辑
        return ApiRes.ok();
    }

    /**
     * 商户信息查询接口 - 可选验签
     */
    @SignRequired(required = false)
    @GetMapping("/info")
    public ApiRes getMchInfo(String mchNo) {
        // 业务逻辑
        return ApiRes.ok();
    }
}
```

---

## 配置说明

### 配置项详解

| 配置项 | 类型 | 默认值 | 说明 |
|-------|-----|--------|------|
| `jeepay.signature.enabled` | boolean | false | 是否全局启用签名验证 |
| `jeepay.signature.default-algorithm` | String | MD5 | 默认签名算法: MD5, SHA256 |
| `jeepay.signature.default-secret` | String | - | 系统级默认密钥 |
| `jeepay.signature.time-window` | long | 300000 | 时间窗口(毫秒),默认5分钟 |
| `jeepay.signature.strict-mode` | boolean | false | 是否启用严格模式(nonce去重) |

### 注解属性说明

`@SignRequired`注解支持以下属性:

| 属性 | 类型 | 默认值 | 说明 |
|-----|-----|--------|------|
| `required` | boolean | true | 是否必须验签 |
| `algorithm` | String | MD5 | 签名算法类型 |
| `checkTimestamp` | boolean | true | 是否校验时间戳 |
| `checkNonce` | boolean | false | 是否启用nonce去重 |

---

## 接口标注

### 使用场景

#### 1. 类级别标注(所有方法生效)

```java
@RestController
@RequestMapping("/api/sensitive")
@SignRequired(algorithm = "SHA256")  // 整个Controller启用SHA256签名
public class SensitiveController {
    // 所有方法都会进行SHA256签名验证
}
```

#### 2. 方法级别标注(覆盖类级别)

```java
@RestController
@RequestMapping("/api/user")
@SignRequired  // 类级别启用MD5签名
public class UserController {

    @SignRequired(algorithm = "SHA256", checkNonce = true)  // 方法级别覆盖
    @PostMapping("/delete")
    public ApiRes deleteUser(Long userId) {
        // 此方法使用SHA256签名和nonce去重
    }
    
    @SignRequired(required = false)  // 此方法不验签
    @GetMapping("/list")
    public ApiRes listUsers() {
        // 查询方法不需要验签
    }
}
```

#### 3. 防重放攻击

对于敏感操作(如转账、删除等),启用nonce去重:

```java
@SignRequired(checkNonce = true)
@PostMapping("/transfer")
public ApiRes transfer(@RequestBody TransferVO transferVO) {
    // 防止重放攻击
}
```

---

## 客户端签名

### 签名算法流程

1. **收集请求参数**
2. **过滤空值参数和sign字段**
3. **按参数名ASCII码排序**
4. **拼接为key1=value1&key2=value2格式**
5. **追加&key=密钥**
6. **执行MD5/SHA256算法**
7. **转为大写十六进制字符串**

### Java客户端示例

```java
import com.jeequan.jeepay.core.utils.SignatureKit;

// 准备请求参数
Map<String, Object> params = new HashMap<>();
params.put("mchNo", "M1000001");
params.put("amount", 10000);
params.put("timestamp", System.currentTimeMillis());
params.put("nonce", UUID.randomUUID().toString().replace("-", ""));
params.put("signType", "MD5");

// 生成签名
String secret = "your_api_secret_key";
String sign = SignatureKit.sign(params, secret, "MD5");
params.put("sign", sign);

// 发送请求
// POST JSON方式
String jsonBody = JSON.toJSONString(params);
// 或 GET方式
String queryString = JeepayKit.genUrlParams(params);
```

### JavaScript客户端示例

```javascript
const CryptoJS = require('crypto-js');

// 准备请求参数
const params = {
    mchNo: 'M1000001',
    amount: 10000,
    timestamp: Date.now(),
    nonce: generateNonce(),
    signType: 'MD5'
};

// 生成签名
function generateSign(params, secret) {
    // 1. 过滤空值和sign字段
    const filteredParams = {};
    Object.keys(params).forEach(key => {
        if (params[key] !== null && params[key] !== '' && key !== 'sign') {
            filteredParams[key] = params[key];
        }
    });
    
    // 2. 按key排序
    const sortedKeys = Object.keys(filteredParams).sort();
    
    // 3. 拼接字符串
    let signStr = '';
    sortedKeys.forEach(key => {
        signStr += `${key}=${filteredParams[key]}&`;
    });
    signStr += `key=${secret}`;
    
    // 4. MD5计算
    const sign = CryptoJS.MD5(signStr).toString().toUpperCase();
    
    return sign;
}

// 生成nonce
function generateNonce() {
    return Math.random().toString(36).substring(2, 18);
}

// 使用示例
const secret = 'your_api_secret_key';
const sign = generateSign(params, secret);
params.sign = sign;

// 发送请求
axios.post('/api/mch/update', params);
```

### Python客户端示例

```python
import hashlib
import time
import uuid

def generate_sign(params, secret, sign_type='MD5'):
    """生成签名"""
    # 1. 过滤空值和sign字段
    filtered_params = {k: v for k, v in params.items() 
                      if v is not None and v != '' and k != 'sign'}
    
    # 2. 按key排序
    sorted_keys = sorted(filtered_params.keys())
    
    # 3. 拼接字符串
    sign_str = '&'.join([f'{k}={filtered_params[k]}' for k in sorted_keys])
    sign_str += f'&key={secret}'
    
    # 4. 计算哈希
    if sign_type == 'MD5':
        return hashlib.md5(sign_str.encode('utf-8')).hexdigest().upper()
    elif sign_type == 'SHA256':
        return hashlib.sha256(sign_str.encode('utf-8')).hexdigest().upper()
    
# 使用示例
params = {
    'mchNo': 'M1000001',
    'amount': 10000,
    'timestamp': int(time.time() * 1000),
    'nonce': uuid.uuid4().hex[:16],
    'signType': 'MD5'
}

secret = 'your_api_secret_key'
sign = generate_sign(params, secret)
params['sign'] = sign

# 发送请求
import requests
response = requests.post('http://localhost:9217/api/mch/update', json=params)
```

---

## 测试验证

### 1. 单元测试

运行签名工具类测试:

```bash
mvn test -Dtest=SignatureKitTest -pl jeepay-core
```

### 2. 接口测试示例

**请求示例(GET方式)**:

```
GET /api/mch/info?mchNo=M1000001&timestamp=1622016572190&nonce=abc123&signType=MD5&sign=4A5078DABBCE0D9C4E7668DACB96FF7A
```

**请求示例(POST JSON方式)**:

```json
POST /api/mch/update
Content-Type: application/json

{
  "mchNo": "M1000001",
  "mchName": "测试商户",
  "timestamp": 1622016572190,
  "nonce": "abc123xyz",
  "signType": "MD5",
  "sign": "4A5078DABBCE0D9C4E7668DACB96FF7A"
}
```

**成功响应**:

```json
{
  "code": 0,
  "msg": "SUCCESS",
  "data": { ... }
}
```

**失败响应示例**:

| 错误场景 | HTTP状态码 | 业务码 | 错误信息 |
|---------|-----------|-------|---------|
| 缺少签名参数 | 400 | 1001 | 缺少必要的签名参数 |
| 签名验证失败 | 401 | 1002 | 签名验证失败 |
| 时间戳过期 | 401 | 1003 | 请求已过期 |
| 重放攻击 | 401 | 1004 | 检测到重放攻击 |
| 签名算法不支持 | 400 | 1005 | 不支持的签名算法 |

---

## 常见问题

### Q1: 如何为现有用户生成API密钥?

**方法1: 通过SQL批量生成**

```sql
UPDATE t_sys_user 
SET api_secret = REPLACE(UUID(), '-', ''),
    secret_status = 1,
    secret_create_time = NOW()
WHERE api_secret IS NULL;
```

**方法2: 通过代码生成**

```java
import com.jeequan.jeepay.core.utils.JeepayKit;

String apiSecret = JeepayKit.generateApiSecret();
// 保存到数据库
sysUser.setApiSecret(apiSecret);
sysUser.setSecretStatus((byte)1);
sysUser.setSecretCreateTime(new Date());
sysUserService.updateById(sysUser);
```

### Q2: 时间戳验证失败怎么办?

**原因**: 客户端与服务器时间不同步,差值超过时间窗口(默认5分钟)

**解决方案**:
1. 同步客户端与服务器时间
2. 调整时间窗口配置: `jeepay.signature.time-window: 600000` (10分钟)

### Q3: 如何排查签名验证失败?

**步骤**:
1. 开启DEBUG日志查看待签名字符串
2. 检查参数顺序(应按ASCII码排序)
3. 检查密钥是否正确
4. 检查是否包含空值参数
5. 使用提供的工具类生成签名对比

**日志配置**:

```yaml
logging:
  level:
    com.jeequan.jeepay.core.utils.SignatureKit: DEBUG
    com.jeequan.jeepay.core.aop.SignatureInterceptor: DEBUG
```

### Q4: 如何禁用特定接口的签名验证?

**方法1: 注解方式**

```java
@SignRequired(required = false)
@GetMapping("/public/info")
public ApiRes getPublicInfo() {
    // 此接口不验签
}
```

**方法2: 路径排除**

在`WebmvcConfig`中排除路径:

```java
registry.addInterceptor(signatureInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/anon/**", "/api/public/**");
```

### Q5: 如何切换到用户级密钥?

修改`SignatureInterceptor`,从用户信息中获取密钥:

```java
// 获取当前用户
JeeUserDetails userDetails = JeeUserDetails.getCurrentUserDetails();
if (userDetails != null) {
    SysUser user = userDetails.getSysUser();
    String userSecret = user.getApiSecret();
    if (StringUtils.isNotBlank(userSecret) && user.getSecretStatus() == 1) {
        // 使用用户专属密钥验签
        signatureValidator.validate(params, userSecret, checkTimestamp, checkNonce);
        return true;
    }
}
// 否则使用系统默认密钥
signatureValidator.validate(params, defaultSecret, checkTimestamp, checkNonce);
```

### Q6: 严格模式(nonce去重)的性能影响?

**影响**: 每次请求都会查询和写入Redis

**建议**:
- 普通查询接口不启用严格模式
- 敏感操作接口(转账、删除等)启用严格模式
- 确保Redis性能良好

### Q7: 如何监控签名验证情况?

查看日志记录:

```bash
# 查看签名验证失败日志
grep "签名验证失败" /var/log/jeepay/jeepay-manager.log

# 查看重放攻击日志
grep "重放攻击" /var/log/jeepay/jeepay-manager.log
```

---

## 附录

### 签名示例计算过程

**原始参数**:
```
{
  "mchNo": "M1000001",
  "amount": 10000,
  "timestamp": 1622016572190,
  "nonce": "abc123xyz",
  "signType": "MD5"
}
```

**步骤1 - 过滤并排序**:
```
amount=10000
mchNo=M1000001
nonce=abc123xyz
signType=MD5
timestamp=1622016572190
```

**步骤2 - 拼接字符串**:
```
amount=10000&mchNo=M1000001&nonce=abc123xyz&signType=MD5&timestamp=1622016572190&key=test_secret_key_123
```

**步骤3 - MD5计算**:
```
签名结果: C8F2A9D6B3E4F7A1C5D9E2F0B8A6D4C1
```

---

## 更新日志

- **v1.0** (2025-10-17)
  - 初始版本发布
  - 支持MD5和SHA256签名算法
  - 支持时间戳防重放
  - 支持nonce去重机制

---

## 联系支持

如有问题,请联系技术支持或提交Issue到项目仓库。
