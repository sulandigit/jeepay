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
package com.jeequan.jeepay.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 签名工具类
 * 提供签名生成和验证功能
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025-10-17
 */
@Slf4j
public class SignatureKit {

    private static final String ENCODING_CHARSET = "UTF-8";

    /**
     * 签名算法类型
     */
    public static final String SIGN_TYPE_MD5 = "MD5";
    public static final String SIGN_TYPE_SHA256 = "SHA256";

    /**
     * 计算签名摘要
     * 
     * @param params 参数Map
     * @param key 密钥
     * @param signType 签名类型: MD5, SHA256
     * @return 签名值(大写十六进制字符串)
     */
    public static String sign(Map<String, Object> params, String key, String signType) {
        // 1. 过滤空值参数和sign字段
        Map<String, Object> filteredParams = filterParams(params);
        
        // 2. 构建待签名字符串
        String signStr = buildSignStr(filteredParams, key);
        
        log.debug("待签名字符串: {}", signStr);
        
        // 3. 执行签名算法
        String sign = null;
        if (SIGN_TYPE_MD5.equalsIgnoreCase(signType)) {
            sign = md5(signStr, ENCODING_CHARSET);
        } else if (SIGN_TYPE_SHA256.equalsIgnoreCase(signType)) {
            sign = sha256(signStr, ENCODING_CHARSET);
        } else {
            throw new IllegalArgumentException("不支持的签名算法: " + signType);
        }
        
        log.debug("签名结果: {}", sign);
        return sign;
    }

    /**
     * 验证签名
     * 
     * @param params 参数Map(包含sign字段)
     * @param key 密钥
     * @param signType 签名类型
     * @return true-验证通过, false-验证失败
     */
    public static boolean verify(Map<String, Object> params, String key, String signType) {
        if (params == null || !params.containsKey("sign")) {
            return false;
        }
        
        String receivedSign = String.valueOf(params.get("sign"));
        if (StringUtils.isBlank(receivedSign)) {
            return false;
        }
        
        // 移除sign参数后重新计算签名
        Map<String, Object> paramsWithoutSign = new HashMap<>(params);
        paramsWithoutSign.remove("sign");
        
        String calculatedSign = sign(paramsWithoutSign, key, signType);
        
        return receivedSign.equalsIgnoreCase(calculatedSign);
    }

    /**
     * 过滤参数: 移除空值和sign字段
     */
    private static Map<String, Object> filterParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, Object> filtered = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 过滤条件: sign字段、null值、空字符串
            if ("sign".equals(key)) {
                continue;
            }
            if (value == null || "".equals(value)) {
                continue;
            }
            
            filtered.put(key, value);
        }
        
        return filtered;
    }

    /**
     * 构建待签名字符串
     * 格式: key1=value1&key2=value2&key=密钥
     */
    private static String buildSignStr(Map<String, Object> params, String key) {
        // 按参数名ASCII码排序
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            Object v = params.get(k);
            sb.append(k).append("=").append(v);
            
            if (i < keys.size() - 1) {
                sb.append("&");
            }
        }
        
        // 追加密钥
        sb.append("&key=").append(key);
        
        return sb.toString();
    }

    /**
     * MD5签名
     */
    private static String md5(String value, String charset) {
        try {
            byte[] data = value.getBytes(charset);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digestData = md.digest(data);
            return toHex(digestData).toUpperCase();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            log.error("MD5签名异常", e);
            throw new RuntimeException("MD5签名失败", e);
        }
    }

    /**
     * SHA256签名
     */
    private static String sha256(String value, String charset) {
        try {
            byte[] data = value.getBytes(charset);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digestData = md.digest(data);
            return toHex(digestData).toUpperCase();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            log.error("SHA256签名异常", e);
            throw new RuntimeException("SHA256签名失败", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String toHex(byte[] input) {
        if (input == null) {
            return null;
        }
        StringBuilder output = new StringBuilder(input.length * 2);
        for (byte b : input) {
            int current = b & 0xff;
            if (current < 16) {
                output.append("0");
            }
            output.append(Integer.toString(current, 16));
        }
        return output.toString();
    }

    /**
     * 从请求参数中提取签名相关参数
     * 
     * @param allParams 所有参数
     * @return 签名参数Map
     */
    public static Map<String, Object> extractSignParams(Map<String, Object> allParams) {
        Map<String, Object> signParams = new HashMap<>(allParams);
        
        // 移除文件上传类参数等不参与签名的参数
        // 这里可以根据实际需求扩展
        
        return signParams;
    }
}
