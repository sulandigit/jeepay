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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 布隆过滤器配置属性
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025
 */
@Component
@ConfigurationProperties(prefix = "jeepay.bloomfilter")
public class BloomFilterProperties {

    /**
     * 是否启用布隆过滤器
     */
    private Boolean enabled = true;

    /**
     * 预期插入数量
     */
    private Long expectedInsertions = 10000L;

    /**
     * 错误率
     */
    private Double falsePositiveProbability = 0.01;

    /**
     * Redis key前缀
     */
    private String keyPrefix = "jeepay:bloomfilter:";

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getExpectedInsertions() {
        return expectedInsertions;
    }

    public void setExpectedInsertions(Long expectedInsertions) {
        this.expectedInsertions = expectedInsertions;
    }

    public Double getFalsePositiveProbability() {
        return falsePositiveProbability;
    }

    public void setFalsePositiveProbability(Double falsePositiveProbability) {
        this.falsePositiveProbability = falsePositiveProbability;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}
