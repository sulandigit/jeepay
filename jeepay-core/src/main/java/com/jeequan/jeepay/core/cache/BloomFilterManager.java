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

import com.jeequan.jeepay.core.utils.SpringBeansUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;

import java.util.Collection;

/**
 * 布隆过滤器管理器
 * 
 * 用于防止缓存穿透，快速判断数据是否存在
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025-10-15
 */
@Slf4j
public class BloomFilterManager {

    private static RedissonClient redissonClient = null;

    /**
     * 获取RedissonClient实例
     */
    private static RedissonClient getRedissonClient() {
        if (redissonClient == null) {
            try {
                redissonClient = SpringBeansUtil.getBean(RedissonClient.class);
            } catch (Exception e) {
                log.warn("RedissonClient not available, bloom filter will be disabled: {}", e.getMessage());
                return null;
            }
        }
        return redissonClient;
    }

    /**
     * 初始化布隆过滤器
     * @param filterName 过滤器名称
     * @param expectedInsertions 预期插入元素数量
     * @param fpp 期望误判率 (0-1之间，推荐0.01)
     * @return 布隆过滤器实例
     */
    public static <T> RBloomFilter<T> initBloomFilter(String filterName, long expectedInsertions, double fpp) {
        RedissonClient client = getRedissonClient();
        if (client == null) {
            log.warn("RedissonClient not available, cannot initialize bloom filter: {}", filterName);
            return null;
        }

        try {
            RBloomFilter<T> bloomFilter = client.getBloomFilter(filterName);
            
            // 如果布隆过滤器已存在且配置匹配，直接返回
            if (bloomFilter.isExists()) {
                log.info("Bloom filter [{}] already exists, reusing it", filterName);
                return bloomFilter;
            }

            // 初始化布隆过滤器
            bloomFilter.tryInit(expectedInsertions, fpp);
            log.info("Bloom filter [{}] initialized successfully, expectedSize: {}, fpp: {}", 
                    filterName, expectedInsertions, fpp);
            return bloomFilter;
        } catch (Exception e) {
            log.error("Failed to initialize bloom filter [{}]: {}", filterName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取已存在的布隆过滤器
     * @param filterName 过滤器名称
     * @return 布隆过滤器实例，如果不存在返回null
     */
    public static <T> RBloomFilter<T> getBloomFilter(String filterName) {
        RedissonClient client = getRedissonClient();
        if (client == null) {
            return null;
        }

        try {
            RBloomFilter<T> bloomFilter = client.getBloomFilter(filterName);
            return bloomFilter.isExists() ? bloomFilter : null;
        } catch (Exception e) {
            log.error("Failed to get bloom filter [{}]: {}", filterName, e.getMessage());
            return null;
        }
    }

    /**
     * 添加元素到布隆过滤器
     * @param filterName 过滤器名称
     * @param element 要添加的元素
     * @return 添加成功返回true
     */
    public static <T> boolean add(String filterName, T element) {
        if (element == null) {
            return false;
        }

        RBloomFilter<T> bloomFilter = getBloomFilter(filterName);
        if (bloomFilter == null) {
            log.warn("Bloom filter [{}] not found, cannot add element", filterName);
            return false;
        }

        try {
            return bloomFilter.add(element);
        } catch (Exception e) {
            log.error("Failed to add element to bloom filter [{}]: {}", filterName, e.getMessage());
            return false;
        }
    }

    /**
     * 批量添加元素到布隆过滤器
     * @param filterName 过滤器名称
     * @param elements 要添加的元素集合
     * @return 成功添加的元素数量
     */
    public static <T> long addBatch(String filterName, Collection<T> elements) {
        if (elements == null || elements.isEmpty()) {
            return 0;
        }

        RBloomFilter<T> bloomFilter = getBloomFilter(filterName);
        if (bloomFilter == null) {
            log.warn("Bloom filter [{}] not found, cannot add elements", filterName);
            return 0;
        }

        try {
            long count = 0;
            for (T element : elements) {
                if (element != null && bloomFilter.add(element)) {
                    count++;
                }
            }
            log.info("Added {} elements to bloom filter [{}]", count, filterName);
            return count;
        } catch (Exception e) {
            log.error("Failed to add batch elements to bloom filter [{}]: {}", filterName, e.getMessage());
            return 0;
        }
    }

    /**
     * 检查元素是否可能存在于布隆过滤器中
     * @param filterName 过滤器名称
     * @param element 要检查的元素
     * @return true表示可能存在，false表示一定不存在
     */
    public static <T> boolean mightContain(String filterName, T element) {
        if (element == null) {
            return false;
        }

        RBloomFilter<T> bloomFilter = getBloomFilter(filterName);
        if (bloomFilter == null) {
            // 如果布隆过滤器不可用，默认返回true，让查询继续进行
            log.warn("Bloom filter [{}] not available, allowing query to proceed", filterName);
            return true;
        }

        try {
            return bloomFilter.contains(element);
        } catch (Exception e) {
            log.error("Failed to check element in bloom filter [{}]: {}", filterName, e.getMessage());
            // 发生异常时，为保证业务正常运行，返回true
            return true;
        }
    }

    /**
     * 重建布隆过滤器
     * @param filterName 过滤器名称
     * @param expectedInsertions 预期插入元素数量
     * @param fpp 期望误判率
     * @return 新的布隆过滤器实例
     */
    public static <T> RBloomFilter<T> rebuildBloomFilter(String filterName, long expectedInsertions, double fpp) {
        RedissonClient client = getRedissonClient();
        if (client == null) {
            return null;
        }

        try {
            // 删除旧的布隆过滤器
            RBloomFilter<T> oldFilter = client.getBloomFilter(filterName);
            if (oldFilter.isExists()) {
                oldFilter.delete();
                log.info("Deleted old bloom filter [{}]", filterName);
            }

            // 创建新的布隆过滤器
            return initBloomFilter(filterName, expectedInsertions, fpp);
        } catch (Exception e) {
            log.error("Failed to rebuild bloom filter [{}]: {}", filterName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除布隆过滤器
     * @param filterName 过滤器名称
     * @return 删除成功返回true
     */
    public static boolean deleteBloomFilter(String filterName) {
        RedissonClient client = getRedissonClient();
        if (client == null) {
            return false;
        }

        try {
            RBloomFilter<?> bloomFilter = client.getBloomFilter(filterName);
            if (bloomFilter.isExists()) {
                boolean result = bloomFilter.delete();
                log.info("Bloom filter [{}] deleted: {}", filterName, result);
                return result;
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to delete bloom filter [{}]: {}", filterName, e.getMessage());
            return false;
        }
    }

    /**
     * 获取布隆过滤器中的元素数量（近似值）
     * @param filterName 过滤器名称
     * @return 元素数量
     */
    public static long count(String filterName) {
        RBloomFilter<?> bloomFilter = getBloomFilter(filterName);
        if (bloomFilter == null) {
            return 0;
        }

        try {
            return bloomFilter.count();
        } catch (Exception e) {
            log.error("Failed to get count from bloom filter [{}]: {}", filterName, e.getMessage());
            return 0;
        }
    }

    // ================ 商户编号布隆过滤器 ================

    /**
     * 初始化商户编号布隆过滤器
     * @param expectedSize 预期商户数量
     */
    public static RBloomFilter<String> initMchNoBloomFilter(long expectedSize) {
        return initBloomFilter(CacheKeyManager.getBloomMchNoKey(), expectedSize, 0.01);
    }

    /**
     * 检查商户编号是否可能存在
     */
    public static boolean mchNoMightExist(String mchNo) {
        return mightContain(CacheKeyManager.getBloomMchNoKey(), mchNo);
    }

    /**
     * 添加商户编号到布隆过滤器
     */
    public static boolean addMchNo(String mchNo) {
        return add(CacheKeyManager.getBloomMchNoKey(), mchNo);
    }

    // ================ 商户应用ID布隆过滤器 ================

    /**
     * 初始化商户应用ID布隆过滤器
     * @param expectedSize 预期应用数量
     */
    public static RBloomFilter<String> initMchAppBloomFilter(long expectedSize) {
        return initBloomFilter(CacheKeyManager.getBloomMchAppKey(), expectedSize, 0.01);
    }

    /**
     * 检查商户应用ID是否可能存在
     */
    public static boolean mchAppMightExist(String appId) {
        return mightContain(CacheKeyManager.getBloomMchAppKey(), appId);
    }

    /**
     * 添加商户应用ID到布隆过滤器
     */
    public static boolean addMchApp(String appId) {
        return add(CacheKeyManager.getBloomMchAppKey(), appId);
    }

    // ================ 服务商编号布隆过滤器 ================

    /**
     * 初始化服务商编号布隆过滤器
     * @param expectedSize 预期服务商数量
     */
    public static RBloomFilter<String> initIsvNoBloomFilter(long expectedSize) {
        return initBloomFilter(CacheKeyManager.getBloomIsvNoKey(), expectedSize, 0.01);
    }

    /**
     * 检查服务商编号是否可能存在
     */
    public static boolean isvNoMightExist(String isvNo) {
        return mightContain(CacheKeyManager.getBloomIsvNoKey(), isvNo);
    }

    /**
     * 添加服务商编号到布隆过滤器
     */
    public static boolean addIsvNo(String isvNo) {
        return add(CacheKeyManager.getBloomIsvNoKey(), isvNo);
    }
}
