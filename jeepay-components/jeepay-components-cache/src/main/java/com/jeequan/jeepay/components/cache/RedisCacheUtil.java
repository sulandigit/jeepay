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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类
 * 提供统一的缓存操作接口，包含缓存穿透、雪崩防护
 * 
 * @author optimization
 * @since 2025-10-15
 */
@Component
public class RedisCacheUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** 默认缓存过期时间（秒） */
    private static final long DEFAULT_EXPIRE_TIME = 1800; // 30分钟

    /** 空值缓存过期时间（秒） - 防止缓存穿透 */
    private static final long NULL_CACHE_EXPIRE_TIME = 300; // 5分钟

    /** 随机过期时间范围（秒） - 防止缓存雪崩 */
    private static final int RANDOM_EXPIRE_RANGE = 300; // 0-5分钟

    private static final Random random = new Random();

    /**
     * 设置缓存
     * @param key 缓存Key
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 设置缓存，带过期时间
     * @param key 缓存Key
     * @param value 缓存值
     * @param expireTime 过期时间（秒）
     */
    public void set(String key, Object value, long expireTime) {
        // 添加随机过期时间，防止缓存雪崩
        long finalExpireTime = expireTime + random.nextInt(RANDOM_EXPIRE_RANGE);
        redisTemplate.opsForValue().set(key, value, finalExpireTime, TimeUnit.SECONDS);
    }

    /**
     * 设置空值缓存（防止缓存穿透）
     * @param key 缓存Key
     */
    public void setNull(String key) {
        redisTemplate.opsForValue().set(key, "", NULL_CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存
     * @param key 缓存Key
     * @return 缓存值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取缓存，带类型转换
     * @param key 缓存Key
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (clazz.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 删除缓存
     * @param key 缓存Key
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     * @param keys 缓存Key集合
     */
    public void delete(String... keys) {
        if (keys != null && keys.length > 0) {
            for (String key : keys) {
                redisTemplate.delete(key);
            }
        }
    }

    /**
     * 判断缓存是否存在
     * @param key 缓存Key
     * @return 是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置过期时间
     * @param key 缓存Key
     * @param time 过期时间（秒）
     */
    public void expire(String key, long time) {
        redisTemplate.expire(key, time, TimeUnit.SECONDS);
    }

    /**
     * 获取过期时间
     * @param key 缓存Key
     * @return 剩余过期时间（秒）
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断是否为空值缓存
     * @param value 缓存值
     * @return 是否为空值
     */
    public boolean isNullCache(Object value) {
        return value != null && "".equals(value.toString());
    }
}
