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
package com.jeequan.jeepay.service.impl;

import com.jeequan.jeepay.core.cache.BloomFilterInitService;
import com.jeequan.jeepay.core.cache.BloomFilterManager;
import com.jeequan.jeepay.core.entity.IsvInfo;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.entity.MchInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 布隆过滤器初始化服务实现
 * 
 * 在应用启动时初始化布隆过滤器，加载现有数据
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025-10-15
 */
@Slf4j
@Service
public class BloomFilterInitServiceImpl implements BloomFilterInitService, ApplicationRunner {

    @Autowired
    private MchInfoService mchInfoService;

    @Autowired
    private MchAppService mchAppService;

    @Autowired
    private IsvInfoService isvInfoService;

    /**
     * 应用启动时自动初始化布隆过滤器
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting bloom filter initialization...");
        initBloomFilters();
        log.info("Bloom filter initialization completed");
    }

    /**
     * 初始化所有布隆过滤器
     */
    @Override
    public void initBloomFilters() {
        try {
            // 初始化商户编号布隆过滤器
            initMchNoBloomFilter();
            
            // 初始化商户应用ID布隆过滤器
            initMchAppBloomFilter();
            
            // 初始化服务商编号布隆过滤器
            initIsvNoBloomFilter();
            
        } catch (Exception e) {
            log.error("Failed to initialize bloom filters", e);
        }
    }

    /**
     * 重建所有布隆过滤器
     * 每天凌晨3点执行
     */
    @Override
    @Scheduled(cron = "0 0 3 * * ?")
    public void rebuildBloomFilters() {
        log.info("Starting bloom filter rebuild...");
        try {
            // 重建商户编号布隆过滤器
            rebuildMchNoBloomFilter();
            
            // 重建商户应用ID布隆过滤器
            rebuildMchAppBloomFilter();
            
            // 重建服务商编号布隆过滤器
            rebuildIsvNoBloomFilter();
            
            log.info("Bloom filter rebuild completed");
        } catch (Exception e) {
            log.error("Failed to rebuild bloom filters", e);
        }
    }

    /**
     * 初始化商户编号布隆过滤器
     */
    private void initMchNoBloomFilter() {
        try {
            // 查询所有商户数量
            long mchCount = mchInfoService.count();
            
            // 预留20%的增长空间
            long expectedSize = (long) (mchCount * 1.2);
            expectedSize = Math.max(expectedSize, 100000); // 最少10万
            
            // 初始化布隆过滤器
            BloomFilterManager.initMchNoBloomFilter(expectedSize);
            
            // 分批加载商户编号
            int batchSize = 1000;
            int page = 1;
            while (true) {
                List<MchInfo> mchInfoList = mchInfoService.list(
                    MchInfo.gw()
                        .select(MchInfo::getMchNo)
                        .last("LIMIT " + (page - 1) * batchSize + ", " + batchSize)
                );
                
                if (mchInfoList == null || mchInfoList.isEmpty()) {
                    break;
                }
                
                // 批量添加到布隆过滤器
                List<String> mchNos = mchInfoList.stream()
                    .map(MchInfo::getMchNo)
                    .collect(Collectors.toList());
                
                long addCount = BloomFilterManager.addBatch(
                    BloomFilterManager.getBloomMchNoKey(), 
                    mchNos
                );
                
                log.debug("Added {} merchant numbers to bloom filter (page {})", addCount, page);
                page++;
            }
            
            log.info("Merchant number bloom filter initialized with {} items", mchCount);
        } catch (Exception e) {
            log.error("Failed to initialize merchant number bloom filter", e);
        }
    }

    /**
     * 初始化商户应用ID布隆过滤器
     */
    private void initMchAppBloomFilter() {
        try {
            // 查询所有商户应用数量
            long appCount = mchAppService.count();
            
            // 预留20%的增长空间
            long expectedSize = (long) (appCount * 1.2);
            expectedSize = Math.max(expectedSize, 50000); // 最少5万
            
            // 初始化布隆过滤器
            BloomFilterManager.initMchAppBloomFilter(expectedSize);
            
            // 分批加载商户应用ID
            int batchSize = 1000;
            int page = 1;
            while (true) {
                List<MchApp> mchAppList = mchAppService.list(
                    MchApp.gw()
                        .select(MchApp::getAppId)
                        .last("LIMIT " + (page - 1) * batchSize + ", " + batchSize)
                );
                
                if (mchAppList == null || mchAppList.isEmpty()) {
                    break;
                }
                
                // 批量添加到布隆过滤器
                List<String> appIds = mchAppList.stream()
                    .map(MchApp::getAppId)
                    .collect(Collectors.toList());
                
                long addCount = BloomFilterManager.addBatch(
                    BloomFilterManager.getBloomMchAppKey(), 
                    appIds
                );
                
                log.debug("Added {} app IDs to bloom filter (page {})", addCount, page);
                page++;
            }
            
            log.info("Merchant app bloom filter initialized with {} items", appCount);
        } catch (Exception e) {
            log.error("Failed to initialize merchant app bloom filter", e);
        }
    }

    /**
     * 初始化服务商编号布隆过滤器
     */
    private void initIsvNoBloomFilter() {
        try {
            // 查询所有服务商数量
            long isvCount = isvInfoService.count();
            
            // 预留20%的增长空间
            long expectedSize = (long) (isvCount * 1.2);
            expectedSize = Math.max(expectedSize, 10000); // 最少1万
            
            // 初始化布隆过滤器
            BloomFilterManager.initIsvNoBloomFilter(expectedSize);
            
            // 查询所有服务商编号
            List<IsvInfo> isvInfoList = isvInfoService.list(
                IsvInfo.gw().select(IsvInfo::getIsvNo)
            );
            
            if (isvInfoList != null && !isvInfoList.isEmpty()) {
                // 批量添加到布隆过滤器
                List<String> isvNos = isvInfoList.stream()
                    .map(IsvInfo::getIsvNo)
                    .collect(Collectors.toList());
                
                long addCount = BloomFilterManager.addBatch(
                    BloomFilterManager.getBloomIsvNoKey(), 
                    isvNos
                );
                
                log.info("ISV number bloom filter initialized with {} items", addCount);
            }
        } catch (Exception e) {
            log.error("Failed to initialize ISV number bloom filter", e);
        }
    }

    /**
     * 重建商户编号布隆过滤器
     */
    private void rebuildMchNoBloomFilter() {
        long mchCount = mchInfoService.count();
        long expectedSize = (long) (mchCount * 1.2);
        expectedSize = Math.max(expectedSize, 100000);
        
        // 删除旧的并创建新的
        BloomFilterManager.rebuildBloomFilter(
            BloomFilterManager.getBloomMchNoKey(), 
            expectedSize, 
            0.01
        );
        
        // 重新加载数据
        initMchNoBloomFilter();
    }

    /**
     * 重建商户应用ID布隆过滤器
     */
    private void rebuildMchAppBloomFilter() {
        long appCount = mchAppService.count();
        long expectedSize = (long) (appCount * 1.2);
        expectedSize = Math.max(expectedSize, 50000);
        
        // 删除旧的并创建新的
        BloomFilterManager.rebuildBloomFilter(
            BloomFilterManager.getBloomMchAppKey(), 
            expectedSize, 
            0.01
        );
        
        // 重新加载数据
        initMchAppBloomFilter();
    }

    /**
     * 重建服务商编号布隆过滤器
     */
    private void rebuildIsvNoBloomFilter() {
        long isvCount = isvInfoService.count();
        long expectedSize = (long) (isvCount * 1.2);
        expectedSize = Math.max(expectedSize, 10000);
        
        // 删除旧的并创建新的
        BloomFilterManager.rebuildBloomFilter(
            BloomFilterManager.getBloomIsvNoKey(), 
            expectedSize, 
            0.01
        );
        
        // 重新加载数据
        initIsvNoBloomFilter();
    }
}
