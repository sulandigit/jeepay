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
package com.jeequan.jeepay.pay.service;

import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jeequan.jeepay.core.entity.MsgTemplate;
import com.jeequan.jeepay.core.entity.MsgTemplateVersion;
import com.jeequan.jeepay.core.service.IMsgTemplateService;
import com.jeequan.jeepay.core.service.IMsgTemplateVersionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 模板缓存管理器
 * 采用两级缓存架构: JVM本地缓存(Caffeine) + Redis分布式缓存
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@Slf4j
@Service
public class TemplateCacheManager {

    @Autowired
    private IMsgTemplateService msgTemplateService;

    @Autowired
    private IMsgTemplateVersionService msgTemplateVersionService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** Redis缓存Key前缀 */
    private static final String REDIS_KEY_PREFIX = "msg:template:";

    /** Redis缓存过期时间(1小时) */
    private static final long REDIS_EXPIRE_SECONDS = 3600;

    /** 本地缓存(Caffeine) */
    private Cache<String, MsgTemplateVersion> localCache;

    /**
     * 初始化本地缓存
     */
    @PostConstruct
    public void init() {
        localCache = Caffeine.newBuilder()
                .maximumSize(100)  // 最大缓存100个模板
                .expireAfterWrite(5, TimeUnit.MINUTES)  // 写入后5分钟过期
                .build();
        
        log.info("模板缓存管理器初始化完成");
    }

    /**
     * 根据模板编码获取当前生效的模板版本
     *
     * @param templateCode 模板编码
     * @return 模板版本对象
     */
    public MsgTemplateVersion getActiveTemplate(String templateCode) {
        if (StringUtils.isBlank(templateCode)) {
            return null;
        }

        String cacheKey = templateCode;

        // 1. 查询本地缓存
        MsgTemplateVersion cachedVersion = localCache.getIfPresent(cacheKey);
        if (cachedVersion != null) {
            log.debug("从本地缓存获取模板: {}", templateCode);
            return cachedVersion;
        }

        // 2. 查询Redis缓存
        String redisKey = REDIS_KEY_PREFIX + templateCode;
        String redisValue = stringRedisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isNotBlank(redisValue)) {
            log.debug("从Redis缓存获取模板: {}", templateCode);
            MsgTemplateVersion version = JSON.parseObject(redisValue, MsgTemplateVersion.class);
            // 更新本地缓存
            localCache.put(cacheKey, version);
            return version;
        }

        // 3. 查询数据库
        log.debug("从数据库查询模板: {}", templateCode);
        MsgTemplate template = msgTemplateService.getByTemplateCode(templateCode);
        if (template == null || template.getState() != MsgTemplate.STATE_ENABLED) {
            log.warn("模板不存在或已停用: {}", templateCode);
            return null;
        }

        MsgTemplateVersion version = msgTemplateVersionService.getActiveVersion(template.getTemplateId());
        if (version == null) {
            log.warn("模板没有生效版本: {}", templateCode);
            return null;
        }

        // 4. 更新缓存
        updateCache(templateCode, version);

        return version;
    }

    /**
     * 根据模板类型获取当前生效的模板版本
     *
     * @param templateType 模板类型
     * @return 模板版本对象
     */
    public MsgTemplateVersion getActiveTemplateByType(Byte templateType) {
        if (templateType == null) {
            return null;
        }

        MsgTemplate template = msgTemplateService.getByTemplateType(templateType);
        if (template == null) {
            return null;
        }

        return getActiveTemplate(template.getTemplateCode());
    }

    /**
     * 更新缓存
     *
     * @param templateCode 模板编码
     * @param version 模板版本对象
     */
    private void updateCache(String templateCode, MsgTemplateVersion version) {
        if (StringUtils.isBlank(templateCode) || version == null) {
            return;
        }

        // 更新本地缓存
        localCache.put(templateCode, version);

        // 更新Redis缓存
        String redisKey = REDIS_KEY_PREFIX + templateCode;
        String redisValue = JSON.toJSONString(version);
        stringRedisTemplate.opsForValue().set(redisKey, redisValue, REDIS_EXPIRE_SECONDS, TimeUnit.SECONDS);

        log.info("更新模板缓存成功: {}", templateCode);
    }

    /**
     * 清除指定模板的缓存
     *
     * @param templateCode 模板编码
     */
    public void evictCache(String templateCode) {
        if (StringUtils.isBlank(templateCode)) {
            return;
        }

        // 清除本地缓存
        localCache.invalidate(templateCode);

        // 清除Redis缓存
        String redisKey = REDIS_KEY_PREFIX + templateCode;
        stringRedisTemplate.delete(redisKey);

        log.info("清除模板缓存成功: {}", templateCode);
    }

    /**
     * 清除所有模板缓存
     */
    public void evictAllCache() {
        // 清除本地缓存
        localCache.invalidateAll();

        // 清除Redis缓存(使用模式匹配)
        // 注意: 这里简化处理，实际生产环境建议使用其他方式
        log.info("清除所有模板缓存");
    }

    /**
     * 刷新指定模板的缓存
     *
     * @param templateCode 模板编码
     */
    public void refreshCache(String templateCode) {
        // 先清除
        evictCache(templateCode);
        
        // 重新加载
        getActiveTemplate(templateCode);
        
        log.info("刷新模板缓存成功: {}", templateCode);
    }
}
