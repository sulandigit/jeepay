/*
 * Copyright (c) 2021-2031, 河北计全科技有限公司 (https://www.jeequan.com & jeequan@126.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeequan.jeepay.core.cache;

/**
 * 缓存Key统一管理器
 * 
 * 提供统一的缓存Key命名规范和生成方法
 * Key命名规范: {业务域}:{实体类型}:{唯一标识}:{附加信息}
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025-10-15
 */
public class CacheKeyManager {

    // ================ 缓存Key前缀常量 ================
    
    /** 用户认证相关前缀 */
    private static final String PREFIX_AUTH = "auth";
    
    /** 商户信息相关前缀 */
    private static final String PREFIX_MCH = "mch";
    
    /** 服务商信息相关前缀 */
    private static final String PREFIX_ISV = "isv";
    
    /** 支付配置相关前缀 */
    private static final String PREFIX_PAY = "pay";
    
    /** 系统配置相关前缀 */
    private static final String PREFIX_SYS = "sys";
    
    /** 权限数据相关前缀 */
    private static final String PREFIX_PERMISSION = "permission";

    /** 布隆过滤器前缀 */
    private static final String PREFIX_BLOOM = "bloom";

    // ================ 缓存Key模板常量 ================
    
    /** 用户登录Token Key模板: auth:token:{userId}:{uuid} */
    public static final String KEY_AUTH_TOKEN = PREFIX_AUTH + ":token:%s:%s";
    
    /** 图形验证码 Key模板: auth:imgcode:{token} */
    public static final String KEY_AUTH_IMG_CODE = PREFIX_AUTH + ":imgcode:%s";
    
    /** 商户主体信息 Key模板: mch:info:{mchNo} */
    public static final String KEY_MCH_INFO = PREFIX_MCH + ":info:%s";
    
    /** 商户应用信息 Key模板: mch:app:{appId} */
    public static final String KEY_MCH_APP = PREFIX_MCH + ":app:%s";
    
    /** 商户应用配置上下文 Key模板: mch:app:config:{appId} */
    public static final String KEY_MCH_APP_CONFIG = PREFIX_MCH + ":app:config:%s";
    
    /** 支付接口配置 Key模板: pay:if:config:{infoType}:{infoId}:{ifCode} */
    public static final String KEY_PAY_IF_CONFIG = PREFIX_PAY + ":if:config:%s:%s:%s";
    
    /** 商户支付通道 Key模板: pay:passage:{appId}:{wayCode} */
    public static final String KEY_PAY_PASSAGE = PREFIX_PAY + ":passage:%s:%s";
    
    /** 服务商信息 Key模板: isv:info:{isvNo} */
    public static final String KEY_ISV_INFO = PREFIX_ISV + ":info:%s";
    
    /** 服务商配置上下文 Key模板: isv:config:{isvNo} */
    public static final String KEY_ISV_CONFIG = PREFIX_ISV + ":config:%s";
    
    /** 系统配置组 Key模板: sys:config:{groupKey} */
    public static final String KEY_SYS_CONFIG = PREFIX_SYS + ":config:%s";
    
    /** 支付接口定义 Key模板: sys:if:define:{ifCode} */
    public static final String KEY_SYS_IF_DEFINE = PREFIX_SYS + ":if:define:%s";
    
    /** 用户权限集合 Key模板: permission:user:{userId} */
    public static final String KEY_PERMISSION_USER = PREFIX_PERMISSION + ":user:%s";
    
    /** 角色权限映射 Key模板: permission:role:{roleId} */
    public static final String KEY_PERMISSION_ROLE = PREFIX_PERMISSION + ":role:%s";

    /** 商户编号布隆过滤器 Key模板: bloom:mch:no */
    public static final String KEY_BLOOM_MCH_NO = PREFIX_BLOOM + ":mch:no";

    /** 商户应用ID布隆过滤器 Key模板: bloom:mch:app */
    public static final String KEY_BLOOM_MCH_APP = PREFIX_BLOOM + ":mch:app";

    /** 服务商编号布隆过滤器 Key模板: bloom:isv:no */
    public static final String KEY_BLOOM_ISV_NO = PREFIX_BLOOM + ":isv:no";

    /** 支付订单号布隆过滤器 Key模板: bloom:pay:order */
    public static final String KEY_BLOOM_PAY_ORDER = PREFIX_BLOOM + ":pay:order";

    /** 退款订单号布隆过滤器 Key模板: bloom:refund:order */
    public static final String KEY_BLOOM_REFUND_ORDER = PREFIX_BLOOM + ":refund:order";

    // ================ 缓存过期时间常量(秒) ================
    
    /** 用户Token过期时间: 2小时 */
    public static final long TTL_TOKEN = 60 * 60 * 2;
    
    /** 图形验证码过期时间: 1分钟 */
    public static final long TTL_IMG_CODE = 60;
    
    /** 商户信息过期时间: 15分钟 */
    public static final long TTL_MCH_INFO = 60 * 15;
    
    /** 商户应用信息过期时间: 30分钟 */
    public static final long TTL_MCH_APP = 60 * 30;
    
    /** 商户应用配置上下文过期时间: 30分钟 */
    public static final long TTL_MCH_APP_CONFIG = 60 * 30;
    
    /** 服务商信息过期时间: 15分钟 */
    public static final long TTL_ISV_INFO = 60 * 15;
    
    /** 服务商配置上下文过期时间: 30分钟 */
    public static final long TTL_ISV_CONFIG = 60 * 30;
    
    /** 支付接口配置过期时间: 30分钟 */
    public static final long TTL_PAY_IF_CONFIG = 60 * 30;
    
    /** 系统配置过期时间: 1小时 */
    public static final long TTL_SYS_CONFIG = 60 * 60;
    
    /** 用户权限信息过期时间: 2小时 */
    public static final long TTL_USER_PERMISSION = 60 * 60 * 2;

    /** 空值缓存过期时间: 5分钟 */
    public static final long TTL_NULL_VALUE = 60 * 5;

    // ================ 缓存Key生成方法 ================
    
    /**
     * 生成用户Token缓存Key
     * @param userId 用户ID
     * @param uuid UUID
     * @return auth:token:{userId}:{uuid}
     */
    public static String genAuthTokenKey(Long userId, String uuid) {
        return String.format(KEY_AUTH_TOKEN, userId, uuid);
    }
    
    /**
     * 生成图形验证码缓存Key
     * @param token 验证码token
     * @return auth:imgcode:{token}
     */
    public static String genAuthImgCodeKey(String token) {
        return String.format(KEY_AUTH_IMG_CODE, token);
    }
    
    /**
     * 生成商户信息缓存Key
     * @param mchNo 商户号
     * @return mch:info:{mchNo}
     */
    public static String genMchInfoKey(String mchNo) {
        return String.format(KEY_MCH_INFO, mchNo);
    }
    
    /**
     * 生成商户应用信息缓存Key
     * @param appId 应用ID
     * @return mch:app:{appId}
     */
    public static String genMchAppKey(String appId) {
        return String.format(KEY_MCH_APP, appId);
    }
    
    /**
     * 生成商户应用配置上下文缓存Key
     * @param appId 应用ID
     * @return mch:app:config:{appId}
     */
    public static String genMchAppConfigKey(String appId) {
        return String.format(KEY_MCH_APP_CONFIG, appId);
    }
    
    /**
     * 生成支付接口配置缓存Key
     * @param infoType 信息类型
     * @param infoId 信息ID
     * @param ifCode 接口代码
     * @return pay:if:config:{infoType}:{infoId}:{ifCode}
     */
    public static String genPayIfConfigKey(Byte infoType, String infoId, String ifCode) {
        return String.format(KEY_PAY_IF_CONFIG, infoType, infoId, ifCode);
    }
    
    /**
     * 生成商户支付通道缓存Key
     * @param appId 应用ID
     * @param wayCode 支付方式代码
     * @return pay:passage:{appId}:{wayCode}
     */
    public static String genPayPassageKey(String appId, String wayCode) {
        return String.format(KEY_PAY_PASSAGE, appId, wayCode);
    }
    
    /**
     * 生成服务商信息缓存Key
     * @param isvNo 服务商号
     * @return isv:info:{isvNo}
     */
    public static String genIsvInfoKey(String isvNo) {
        return String.format(KEY_ISV_INFO, isvNo);
    }
    
    /**
     * 生成服务商配置上下文缓存Key
     * @param isvNo 服务商号
     * @return isv:config:{isvNo}
     */
    public static String genIsvConfigKey(String isvNo) {
        return String.format(KEY_ISV_CONFIG, isvNo);
    }
    
    /**
     * 生成系统配置组缓存Key
     * @param groupKey 配置组Key
     * @return sys:config:{groupKey}
     */
    public static String genSysConfigKey(String groupKey) {
        return String.format(KEY_SYS_CONFIG, groupKey);
    }
    
    /**
     * 生成支付接口定义缓存Key
     * @param ifCode 接口代码
     * @return sys:if:define:{ifCode}
     */
    public static String genSysIfDefineKey(String ifCode) {
        return String.format(KEY_SYS_IF_DEFINE, ifCode);
    }
    
    /**
     * 生成用户权限集合缓存Key
     * @param userId 用户ID
     * @return permission:user:{userId}
     */
    public static String genUserPermissionKey(Long userId) {
        return String.format(KEY_PERMISSION_USER, userId);
    }
    
    /**
     * 生成角色权限映射缓存Key
     * @param roleId 角色ID
     * @return permission:role:{roleId}
     */
    public static String genRolePermissionKey(String roleId) {
        return String.format(KEY_PERMISSION_ROLE, roleId);
    }

    // ================ 布隆过滤器Key生成方法 ================

    /**
     * 获取商户编号布隆过滤器Key
     * @return bloom:mch:no
     */
    public static String getBloomMchNoKey() {
        return KEY_BLOOM_MCH_NO;
    }

    /**
     * 获取商户应用ID布隆过滤器Key
     * @return bloom:mch:app
     */
    public static String getBloomMchAppKey() {
        return KEY_BLOOM_MCH_APP;
    }

    /**
     * 获取服务商编号布隆过滤器Key
     * @return bloom:isv:no
     */
    public static String getBloomIsvNoKey() {
        return KEY_BLOOM_ISV_NO;
    }

    /**
     * 获取支付订单号布隆过滤器Key
     * @return bloom:pay:order
     */
    public static String getBloomPayOrderKey() {
        return KEY_BLOOM_PAY_ORDER;
    }

    /**
     * 获取退款订单号布隆过滤器Key
     * @return bloom:refund:order
     */
    public static String getBloomRefundOrderKey() {
        return KEY_BLOOM_REFUND_ORDER;
    }

    /**
     * 计算带随机偏移的过期时间，避免缓存雪崩
     * @param baseTtl 基础过期时间(秒)
     * @param randomRange 随机偏移范围(秒)，实际偏移范围为 [-randomRange, +randomRange]
     * @return 带随机偏移的过期时间
     */
    public static long getTtlWithRandomOffset(long baseTtl, long randomRange) {
        // 生成 [-randomRange, +randomRange] 之间的随机偏移
        long offset = (long) (Math.random() * randomRange * 2) - randomRange;
        return baseTtl + offset;
    }

    /**
     * 获取商户信息缓存过期时间(带随机偏移)
     * @return 过期时间(秒), 范围: 12-18分钟
     */
    public static long getMchInfoTtl() {
        return getTtlWithRandomOffset(TTL_MCH_INFO, 60 * 3);
    }

    /**
     * 获取商户应用信息缓存过期时间(带随机偏移)
     * @return 过期时间(秒), 范围: 25-35分钟
     */
    public static long getMchAppTtl() {
        return getTtlWithRandomOffset(TTL_MCH_APP, 60 * 5);
    }

    /**
     * 获取服务商信息缓存过期时间(带随机偏移)
     * @return 过期时间(秒), 范围: 12-18分钟
     */
    public static long getIsvInfoTtl() {
        return getTtlWithRandomOffset(TTL_ISV_INFO, 60 * 3);
    }

    /**
     * 获取系统配置缓存过期时间(带随机偏移)
     * @return 过期时间(秒), 范围: 50-70分钟
     */
    public static long getSysConfigTtl() {
        return getTtlWithRandomOffset(TTL_SYS_CONFIG, 60 * 10);
    }
}
