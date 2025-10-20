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
package com.jeequan.jeepay.components.bloomfilter.util;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

/**
 * 布隆过滤器工具类
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025
 */
public class BloomFilterUtil {

    /**
     * 计算布隆过滤器所需的bit数组大小
     *
     * @param expectedInsertions 预期插入数量
     * @param falsePositiveProbability 错误率
     * @return bit数组大小
     * @date 2025
     */
    public static long optimalNumOfBits(long expectedInsertions, double falsePositiveProbability) {
        if (falsePositiveProbability == 0) {
            falsePositiveProbability = Double.MIN_VALUE;
        }
        return (long) (-expectedInsertions * Math.log(falsePositiveProbability) / (Math.log(2) * Math.log(2)));
    }

    /**
     * 计算布隆过滤器所需的哈希函数个数
     *
     * @param expectedInsertions 预期插入数量
     * @param numOfBits bit数组大小
     * @return 哈希函数个数
     * @date 2025
     */
    public static int optimalNumOfHashFunctions(long expectedInsertions, long numOfBits) {
        return Math.max(1, (int) Math.round((double) numOfBits / expectedInsertions * Math.log(2)));
    }

    /**
     * 使用murmur3哈希算法计算哈希值
     *
     * @param data 数据
     * @return 哈希值
     * @date 2025
     */
    public static long hash(String data) {
        return Hashing.murmur3_128().hashString(data, StandardCharsets.UTF_8).asLong();
    }

    /**
     * 计算多个哈希值
     *
     * @param data 数据
     * @param numHashFunctions 哈希函数个数
     * @param bitSize bit数组大小
     * @return 哈希值数组
     * @date 2025
     */
    public static long[] getHashIndexes(String data, int numHashFunctions, long bitSize) {
        long[] result = new long[numHashFunctions];
        long hash1 = hash(data);
        long hash2 = hash(data + "salt");
        
        for (int i = 0; i < numHashFunctions; i++) {
            long combinedHash = hash1 + (i * hash2);
            result[i] = (combinedHash & Long.MAX_VALUE) % bitSize;
        }
        
        return result;
    }

    /**
     * 计算bit位置对应的Redis offset
     *
     * @param bitIndex bit索引
     * @return Redis offset值
     * @date 2025
     */
    public static long getRedisOffset(long bitIndex) {
        return bitIndex;
    }
}
