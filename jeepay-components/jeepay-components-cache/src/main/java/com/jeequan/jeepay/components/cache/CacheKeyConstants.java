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
package com.jeequan.jeepay.components.cache;

/**
 * 缓存Key常量定义
 * 统一管理所有的缓存Key前缀和命名规则
 * 
 * @author optimization
 * @since 2025-10-15
 */
public class CacheKeyConstants {

    /** 缓存Key前缀 */
    private static final String PREFIX = "jeepay:";

    /** 支付订单缓存Key前缀 */
    public static final String PAY_ORDER_PREFIX = PREFIX + "pay:order:";
    
    /** 商户订单映射缓存Key前缀 */
    public static final String MCH_ORDER_PREFIX = PREFIX + "pay:mch_order:";
    
    /** 统计数据缓存Key前缀 */
    public static final String STAT_PREFIX = PREFIX + "pay:stat:";
    
    /** 布隆过滤器Key */
    public static final String BLOOM_FILTER_KEY = PREFIX + "pay:bloom:order";

    /**
     * 获取支付订单缓存Key
     * @param payOrderId 支付订单号
     * @return 缓存Key
     */
    public static String getPayOrderKey(String payOrderId) {
        return PAY_ORDER_PREFIX + payOrderId;
    }

    /**
     * 获取商户订单映射缓存Key
     * @param mchNo 商户号
     * @param mchOrderNo 商户订单号
     * @return 缓存Key
     */
    public static String getMchOrderKey(String mchNo, String mchOrderNo) {
        return MCH_ORDER_PREFIX + mchNo + ":" + mchOrderNo;
    }

    /**
     * 获取统计数据缓存Key
     * @param mchNo 商户号
     * @param date 日期
     * @return 缓存Key
     */
    public static String getStatKey(String mchNo, String date) {
        return STAT_PREFIX + mchNo + ":" + date;
    }
}
