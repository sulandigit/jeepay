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

/**
 * 布隆过滤器初始化服务接口
 * 
 * 各模块需要实现此接口来初始化自己的布隆过滤器
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025-10-15
 */
public interface BloomFilterInitService {

    /**
     * 初始化布隆过滤器
     * 
     * 在应用启动时调用，用于加载已有数据到布隆过滤器
     */
    void initBloomFilters();

    /**
     * 重建布隆过滤器
     * 
     * 定时任务或手动触发时调用，用于重新构建布隆过滤器
     */
    void rebuildBloomFilters();
}
