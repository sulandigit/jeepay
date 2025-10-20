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
package com.jeequan.jeepay.components.bloomfilter.service;

import com.jeequan.jeepay.components.bloomfilter.config.BloomFilterProperties;
import com.jeequan.jeepay.components.bloomfilter.util.BloomFilterUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 布隆过滤器服务类
 * 基于Redis的Bitmap实现布隆过滤器
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025
 */
@Slf4j
public class BloomFilterService {

    private final StringRedisTemplate stringRedisTemplate;
    private final BloomFilterProperties properties;
    private final long bitSize;
    private final int numHashFunctions;

    public BloomFilterService(StringRedisTemplate stringRedisTemplate, BloomFilterProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
        
        // 计算最优bit数组大小和哈希函数个数
        this.bitSize = BloomFilterUtil.optimalNumOfBits(
            properties.getExpectedInsertions(), 
            properties.getFalsePositiveProbability()
        );
        this.numHashFunctions = BloomFilterUtil.optimalNumOfHashFunctions(
            properties.getExpectedInsertions(), 
            this.bitSize
        );
        
        log.info("布隆过滤器初始化成功: bitSize={}, numHashFunctions={}, expectedInsertions={}, fpp={}", 
            bitSize, numHashFunctions, properties.getExpectedInsertions(), properties.getFalsePositiveProbability());
    }

    /**
     * 添加元素到布隆过滤器
     *
     * @param filterName 过滤器名称
     * @param value 要添加的值
     * @return 是否添加成功
     * @date 2025
     */
    public boolean add(String filterName, String value) {
        String key = buildKey(filterName);
        long[] indexes = BloomFilterUtil.getHashIndexes(value, numHashFunctions, bitSize);
        
        try {
            stringRedisTemplate.executePipelined((RedisConnection connection) -> {
                for (long index : indexes) {
                    connection.setBit(key.getBytes(), index, true);
                }
                return null;
            });
            return true;
        } catch (Exception e) {
            log.error("添加元素到布隆过滤器失败: filterName={}, value={}", filterName, value, e);
            return false;
        }
    }

    /**
     * 批量添加元素到布隆过滤器
     *
     * @param filterName 过滤器名称
     * @param values 要添加的值数组
     * @return 是否添加成功
     * @date 2025
     */
    public boolean addAll(String filterName, String... values) {
        if (values == null || values.length == 0) {
            return false;
        }
        
        String key = buildKey(filterName);
        try {
            stringRedisTemplate.executePipelined((RedisConnection connection) -> {
                for (String value : values) {
                    long[] indexes = BloomFilterUtil.getHashIndexes(value, numHashFunctions, bitSize);
                    for (long index : indexes) {
                        connection.setBit(key.getBytes(), index, true);
                    }
                }
                return null;
            });
            return true;
        } catch (Exception e) {
            log.error("批量添加元素到布隆过滤器失败: filterName={}", filterName, e);
            return false;
        }
    }

    /**
     * 判断元素是否可能存在
     *
     * @param filterName 过滤器名称
     * @param value 要检查的值
     * @return true表示可能存在，false表示一定不存在
     * @date 2025
     */
    public boolean mightContain(String filterName, String value) {
        String key = buildKey(filterName);
        long[] indexes = BloomFilterUtil.getHashIndexes(value, numHashFunctions, bitSize);
        
        try {
            for (long index : indexes) {
                Boolean bit = stringRedisTemplate.execute((RedisConnection connection) -> 
                    connection.getBit(key.getBytes(), index)
                );
                if (bit == null || !bit) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("检查元素是否存在失败: filterName={}, value={}", filterName, value, e);
            return false;
        }
    }

    /**
     * 删除布隆过滤器
     *
     * @param filterName 过滤器名称
     * @return 是否删除成功
     * @date 2025
     */
    public boolean delete(String filterName) {
        String key = buildKey(filterName);
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
        } catch (Exception e) {
            log.error("删除布隆过滤器失败: filterName={}", filterName, e);
            return false;
        }
    }

    /**
     * 设置布隆过滤器过期时间
     *
     * @param filterName 过滤器名称
     * @param timeout 过期时间
     * @param timeUnit 时间单位
     * @return 是否设置成功
     * @date 2025
     */
    public boolean expire(String filterName, long timeout, TimeUnit timeUnit) {
        String key = buildKey(filterName);
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.expire(key, timeout, timeUnit));
        } catch (Exception e) {
            log.error("设置布隆过滤器过期时间失败: filterName={}", filterName, e);
            return false;
        }
    }

    /**
     * 构建Redis key
     *
     * @param filterName 过滤器名称
     * @return Redis key
     * @date 2025
     */
    private String buildKey(String filterName) {
        return properties.getKeyPrefix() + filterName;
    }

    /**
     * 获取bit数组大小
     *
     * @return bit数组大小
     * @date 2025
     */
    public long getBitSize() {
        return bitSize;
    }

    /**
     * 获取哈希函数个数
     *
     * @return 哈希函数个数
     * @date 2025
     */
    public int getNumHashFunctions() {
        return numHashFunctions;
    }
}
