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
package com.jeequan.jeepay.core.service;

import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.exception.InvalidSignatureException;
import com.jeequan.jeepay.core.exception.MissingSignatureException;
import com.jeequan.jeepay.core.exception.UnsupportedAlgorithmException;
import com.jeequan.jeepay.core.utils.SignatureKit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 签名验证服务
 * 核心验签业务逻辑
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025-10-17
 */
@Slf4j
@Service
public class SignatureValidator {

    @Autowired
    private ReplayAttackDefender replayAttackDefender;

    /**
     * 默认签名算法
     */
    @Value("${jeepay.signature.default-algorithm:MD5}")
    private String defaultAlgorithm;

    /**
     * 系统级默认密钥
     */
    @Value("${jeepay.signature.default-secret:jeepay_secret_key_2025}")
    private String defaultSecret;

    /**
     * 验证签名
     * 
     * @param params 请求参数(包含sign, signType, timestamp, nonce等)
     * @param secret API密钥
     * @param checkTimestamp 是否校验时间戳
     * @param checkNonce 是否校验nonce
     * @throws MissingSignatureException 缺少签名参数
     * @throws InvalidSignatureException 签名验证失败
     * @throws UnsupportedAlgorithmException 不支持的签名算法
     */
    public void validate(Map<String, Object> params, String secret, 
                        boolean checkTimestamp, boolean checkNonce) {
        
        // 1. 检查必要参数
        checkRequiredParams(params);

        // 2. 获取签名类型
        String signType = getSignType(params);

        // 3. 验证签名算法是否支持
        validateAlgorithm(signType);

        // 4. 获取密钥
        String signSecret = StringUtils.isNotBlank(secret) ? secret : defaultSecret;

        // 5. 验证签名
        boolean isValid = SignatureKit.verify(params, signSecret, signType);
        if (!isValid) {
            log.warn("签名验证失败, 参数: {}", params);
            throw new InvalidSignatureException(ApiCodeEnum.SIGN_VERIFY_FAILED);
        }

        log.debug("签名验证通过");

        // 6. 验证时间戳和nonce(防重放攻击)
        if (checkTimestamp) {
            Long timestamp = getTimestamp(params);
            String nonce = getNonce(params);
            replayAttackDefender.validate(timestamp, nonce, checkNonce);
        }
    }

    /**
     * 验证签名(使用默认密钥)
     */
    public void validate(Map<String, Object> params, boolean checkTimestamp, boolean checkNonce) {
        validate(params, defaultSecret, checkTimestamp, checkNonce);
    }

    /**
     * 检查必要的签名参数
     */
    private void checkRequiredParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            throw new MissingSignatureException(ApiCodeEnum.SIGN_MISSING_PARAMS);
        }

        if (!params.containsKey("sign") || StringUtils.isBlank(String.valueOf(params.get("sign")))) {
            log.warn("缺少sign参数");
            throw new MissingSignatureException(ApiCodeEnum.SIGN_MISSING_PARAMS);
        }
    }

    /**
     * 获取签名类型
     */
    private String getSignType(Map<String, Object> params) {
        String signType = params.containsKey("signType") ? 
                         String.valueOf(params.get("signType")) : defaultAlgorithm;
        
        if (StringUtils.isBlank(signType)) {
            signType = defaultAlgorithm;
        }

        return signType.toUpperCase();
    }

    /**
     * 验证签名算法是否支持
     */
    private void validateAlgorithm(String signType) {
        if (!SignatureKit.SIGN_TYPE_MD5.equalsIgnoreCase(signType) 
            && !SignatureKit.SIGN_TYPE_SHA256.equalsIgnoreCase(signType)) {
            log.warn("不支持的签名算法: {}", signType);
            throw new UnsupportedAlgorithmException(ApiCodeEnum.SIGN_ALGORITHM_UNSUPPORTED);
        }
    }

    /**
     * 获取时间戳
     */
    private Long getTimestamp(Map<String, Object> params) {
        if (!params.containsKey("timestamp")) {
            return null;
        }

        Object timestampObj = params.get("timestamp");
        if (timestampObj == null) {
            return null;
        }

        try {
            if (timestampObj instanceof Long) {
                return (Long) timestampObj;
            } else if (timestampObj instanceof String) {
                return Long.parseLong((String) timestampObj);
            } else if (timestampObj instanceof Number) {
                return ((Number) timestampObj).longValue();
            }
        } catch (Exception e) {
            log.warn("时间戳参数格式错误: {}", timestampObj, e);
        }

        return null;
    }

    /**
     * 获取nonce
     */
    private String getNonce(Map<String, Object> params) {
        if (!params.containsKey("nonce")) {
            return null;
        }

        Object nonceObj = params.get("nonce");
        return nonceObj != null ? String.valueOf(nonceObj) : null;
    }

    /**
     * 生成签名(供客户端使用)
     * 
     * @param params 参数Map
     * @param secret 密钥
     * @param signType 签名类型
     * @return 签名值
     */
    public String generateSign(Map<String, Object> params, String secret, String signType) {
        String signSecret = StringUtils.isNotBlank(secret) ? secret : defaultSecret;
        String type = StringUtils.isNotBlank(signType) ? signType : defaultAlgorithm;
        
        return SignatureKit.sign(params, signSecret, type);
    }
}
