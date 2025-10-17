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

import com.jeequan.jeepay.core.cache.RedisUtil;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.exception.ReplayAttackException;
import com.jeequan.jeepay.core.exception.TimestampExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 防重放攻击组件
 * 基于时间戳和随机数(nonce)机制
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025-10-17
 */
@Slf4j
@Service
public class ReplayAttackDefender {

    /**
     * 时间窗口(毫秒), 默认5分钟
     */
    @Value("${jeepay.signature.time-window:300000}")
    private long timeWindow;

    /**
     * 是否启用严格模式(nonce去重)
     */
    @Value("${jeepay.signature.strict-mode:false}")
    private boolean strictMode;

    /**
     * Redis缓存key前缀
     */
    private static final String NONCE_CACHE_PREFIX = "nonce:";

    /**
     * 验证时间戳是否在有效窗口内
     * 
     * @param timestamp 请求时间戳(13位毫秒)
     * @throws TimestampExpiredException 时间戳过期异常
     */
    public void checkTimestamp(Long timestamp) {
        if (timestamp == null) {
            log.warn("时间戳参数为空");
            throw new TimestampExpiredException(ApiCodeEnum.SIGN_TIMESTAMP_EXPIRED);
        }

        long currentTime = System.currentTimeMillis();
        long timeDiff = Math.abs(currentTime - timestamp);

        if (timeDiff > timeWindow) {
            log.warn("请求已过期, 当前时间: {}, 请求时间: {}, 时间差: {}ms", 
                    currentTime, timestamp, timeDiff);
            throw new TimestampExpiredException(ApiCodeEnum.SIGN_TIMESTAMP_EXPIRED);
        }

        log.debug("时间戳验证通过, 时间差: {}ms", timeDiff);
    }

    /**
     * 验证nonce是否重复(防重放攻击)
     * 
     * @param nonce 随机字符串
     * @param timestamp 请求时间戳
     * @throws ReplayAttackException 重放攻击异常
     */
    public void checkNonce(String nonce, Long timestamp) {
        if (!strictMode) {
            log.debug("未启用严格模式, 跳过nonce验证");
            return;
        }

        if (StringUtils.isBlank(nonce)) {
            log.warn("严格模式下nonce参数为空");
            throw new ReplayAttackException(ApiCodeEnum.SIGN_REPLAY_ATTACK);
        }

        if (timestamp == null) {
            log.warn("严格模式下timestamp参数为空");
            throw new ReplayAttackException(ApiCodeEnum.SIGN_REPLAY_ATTACK);
        }

        // 构建缓存key: nonce:{nonce}:{timestamp}
        String cacheKey = buildNonceCacheKey(nonce, timestamp);

        // 检查nonce是否已存在
        if (RedisUtil.hasKey(cacheKey)) {
            log.error("检测到重放攻击, nonce: {}, timestamp: {}", nonce, timestamp);
            throw new ReplayAttackException(ApiCodeEnum.SIGN_REPLAY_ATTACK);
        }

        // 存储nonce到Redis, 过期时间设置为时间窗口
        RedisUtil.setString(cacheKey, "1", timeWindow, TimeUnit.MILLISECONDS);
        
        log.debug("nonce验证通过并缓存, key: {}, 过期时间: {}ms", cacheKey, timeWindow);
    }

    /**
     * 验证时间戳和nonce
     * 
     * @param timestamp 请求时间戳
     * @param nonce 随机字符串
     * @param checkNonce 是否检查nonce
     */
    public void validate(Long timestamp, String nonce, boolean checkNonce) {
        // 1. 验证时间戳
        checkTimestamp(timestamp);

        // 2. 验证nonce(如果启用)
        if (checkNonce || strictMode) {
            checkNonce(nonce, timestamp);
        }
    }

    /**
     * 构建nonce缓存key
     */
    private String buildNonceCacheKey(String nonce, Long timestamp) {
        return NONCE_CACHE_PREFIX + nonce + ":" + timestamp;
    }

    /**
     * 清理过期的nonce缓存(由Redis自动过期机制处理, 此方法预留)
     */
    public void cleanExpiredNonce() {
        // Redis会自动清理过期的key, 无需手动处理
        log.debug("nonce缓存由Redis自动过期机制清理");
    }
}
