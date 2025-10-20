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
package com.jeequan.jeepay.components.bloomfilter.config;

import com.jeequan.jeepay.components.bloomfilter.service.BloomFilterService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 布隆过滤器自动配置类
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025
 */
@Configuration
@ConditionalOnProperty(prefix = "jeepay.bloomfilter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BloomFilterAutoConfiguration {

    /**
     * 创建布隆过滤器服务Bean
     *
     * @param stringRedisTemplate Redis模板
     * @param properties 布隆过滤器配置属性
     * @return 布隆过滤器服务
     * @date 2025
     */
    @Bean
    public BloomFilterService bloomFilterService(StringRedisTemplate stringRedisTemplate, BloomFilterProperties properties) {
        return new BloomFilterService(stringRedisTemplate, properties);
    }
}
