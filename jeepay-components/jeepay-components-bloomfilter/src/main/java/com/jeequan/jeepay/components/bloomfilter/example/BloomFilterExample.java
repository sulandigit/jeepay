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
package com.jeequan.jeepay.components.bloomfilter.example;

import com.jeequan.jeepay.components.bloomfilter.service.BloomFilterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 布隆过滤器使用示例
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025
 */
@Slf4j
@Component
public class BloomFilterExample {

    @Autowired(required = false)
    private BloomFilterService bloomFilterService;

    /**
     * 示例1: 防止缓存穿透
     * 在查询数据库之前,先用布隆过滤器判断数据是否存在
     *
     * @param userId 用户ID
     * @return 是否存在
     * @date 2025
     */
    public boolean checkUserExists(String userId) {
        String filterName = "valid_user_ids";
        
        // 先检查布隆过滤器
        if (!bloomFilterService.mightContain(filterName, userId)) {
            log.info("用户ID {} 在布隆过滤器中不存在,直接返回", userId);
            return false;
        }
        
        // 布隆过滤器中可能存在,需要进一步查询数据库
        log.info("用户ID {} 在布隆过滤器中可能存在,需要查询数据库确认", userId);
        // 这里应该查询数据库,示例省略
        return true;
    }

    /**
     * 示例2: 批量初始化用户ID到布隆过滤器
     *
     * @param userIds 用户ID列表
     * @date 2025
     */
    public void initUserBloomFilter(String... userIds) {
        String filterName = "valid_user_ids";
        
        // 批量添加
        boolean success = bloomFilterService.addAll(filterName, userIds);
        if (success) {
            // 设置过期时间为24小时
            bloomFilterService.expire(filterName, 24, TimeUnit.HOURS);
            log.info("成功初始化 {} 个用户ID到布隆过滤器", userIds.length);
        }
    }

    /**
     * 示例3: 订单去重检查
     *
     * @param orderNo 订单号
     * @return 是否重复
     * @date 2025
     */
    public boolean isDuplicateOrder(String orderNo) {
        String filterName = "order_numbers";
        
        // 检查是否可能存在
        if (bloomFilterService.mightContain(filterName, orderNo)) {
            log.warn("订单号 {} 可能重复,需要数据库确认", orderNo);
            // 这里应该查询数据库确认,示例省略
            return true;
        }
        
        // 不存在,添加到布隆过滤器
        bloomFilterService.add(filterName, orderNo);
        log.info("订单号 {} 不重复,已添加到布隆过滤器", orderNo);
        return false;
    }

    /**
     * 示例4: IP黑名单检查
     *
     * @param ip IP地址
     * @return 是否在黑名单中
     * @date 2025
     */
    public boolean isBlacklistIp(String ip) {
        String filterName = "ip_blacklist";
        
        boolean inBlacklist = bloomFilterService.mightContain(filterName, ip);
        if (inBlacklist) {
            log.warn("IP {} 可能在黑名单中", ip);
        }
        return inBlacklist;
    }

    /**
     * 示例5: 添加IP到黑名单
     *
     * @param ip IP地址
     * @date 2025
     */
    public void addToBlacklist(String ip) {
        String filterName = "ip_blacklist";
        
        bloomFilterService.add(filterName, ip);
        // 设置7天过期
        bloomFilterService.expire(filterName, 7, TimeUnit.DAYS);
        log.info("IP {} 已添加到黑名单布隆过滤器", ip);
    }

    /**
     * 示例6: 清空布隆过滤器
     *
     * @param filterName 过滤器名称
     * @date 2025
     */
    public void clearBloomFilter(String filterName) {
        boolean success = bloomFilterService.delete(filterName);
        if (success) {
            log.info("布隆过滤器 {} 已清空", filterName);
        } else {
            log.error("清空布隆过滤器 {} 失败", filterName);
        }
    }
}
