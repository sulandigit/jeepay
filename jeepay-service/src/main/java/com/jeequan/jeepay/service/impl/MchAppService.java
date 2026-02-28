package com.jeequan.jeepay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.cache.BloomFilterManager;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.entity.MchPayPassage;
import com.jeequan.jeepay.core.entity.PayInterfaceConfig;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.utils.StringKit;
import com.jeequan.jeepay.service.mapper.MchAppMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 商户应用表 服务实现类
 * </p>
 *
 * @author [mybatis plus generator]
 * @since 2021-06-15
 */
@Service
public class MchAppService extends ServiceImpl<MchAppMapper, MchApp> {

    @Autowired private PayOrderService payOrderService;
    @Autowired private MchPayPassageService mchPayPassageService;
    @Autowired private PayInterfaceConfigService payInterfaceConfigService;

    /**
     * 根据ID查询商户应用(带缓存)
     * @param appId 应用ID
     * @return 商户应用信息
     */
    @Override
    @Cacheable(cacheNames = "mchApp", key = "#appId", unless = "#result == null")
    public MchApp getById(String appId) {
        // 先检查布隆过滤器
        if (!BloomFilterManager.mchAppMightExist(appId)) {
            return null;
        }
        return super.getById(appId);
    }

    /**
     * 更新商户应用(同时清除应用和配置缓存)
     * @param entity 商户应用
     * @return 是否更新成功
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "mchApp", key = "#entity.appId"),
            @CacheEvict(cacheNames = "mchAppConfig", key = "#entity.appId")
    })
    public boolean updateById(MchApp entity) {
        return super.updateById(entity);
    }

    /**
     * 保存商户应用(添加到布隆过滤器)
     * @param entity 商户应用
     * @return 是否保存成功
     */
    @Override
    public boolean save(MchApp entity) {
        boolean result = super.save(entity);
        if (result) {
            // 添加到布隆过滤器
            BloomFilterManager.addMchApp(entity.getAppId());
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(cacheNames = "mchApp", key = "#appId"),
            @CacheEvict(cacheNames = "mchAppConfig", key = "#appId")
    })
    public void removeByAppId(String appId) {

        // 1.查看当前应用是否存在交易数据
        int payCount = payOrderService.count(PayOrder.gw().eq(PayOrder::getAppId, appId));
        if (payCount > 0) {
            throw new BizException("该应用已存在交易数据，不可删除");
        }

        // 2.删除应用关联的支付通道
        mchPayPassageService.remove(MchPayPassage.gw().eq(MchPayPassage::getAppId, appId));

        // 3.删除应用配置的支付参数
        payInterfaceConfigService.remove(PayInterfaceConfig.gw()
                .eq(PayInterfaceConfig::getInfoId, appId)
                .eq(PayInterfaceConfig::getInfoType, CS.INFO_TYPE_MCH_APP)
        );

        // 4.删除当前应用
        if (!removeById(appId)) {
            throw new BizException(ApiCodeEnum.SYS_OPERATION_FAIL_DELETE);
        }
    }

    @Cacheable(cacheNames = "mchApp", key = "#appId", unless = "#result == null")
    public MchApp selectById(String appId) {
        MchApp mchApp = this.getById(appId);
        if (mchApp == null) {
            return null;
        }
        mchApp.setAppSecret(StringKit.str2Star(mchApp.getAppSecret(), 6, 6, 6));

        return mchApp;
    }

    public IPage<MchApp> selectPage(IPage iPage, MchApp mchApp) {

        LambdaQueryWrapper<MchApp> wrapper = MchApp.gw();
        if (StringUtils.isNotBlank(mchApp.getMchNo())) {
            wrapper.eq(MchApp::getMchNo, mchApp.getMchNo());
        }
        if (StringUtils.isNotEmpty(mchApp.getAppId())) {
            wrapper.eq(MchApp::getAppId, mchApp.getAppId());
        }
        if (StringUtils.isNotEmpty(mchApp.getAppName())) {
            wrapper.eq(MchApp::getAppName, mchApp.getAppName());
        }
        if (mchApp.getState() != null) {
            wrapper.eq(MchApp::getState, mchApp.getState());
        }
        wrapper.orderByDesc(MchApp::getCreatedAt);

        IPage<MchApp> pages = this.page(iPage, wrapper);

        pages.getRecords().stream().forEach(item -> item.setAppSecret(StringKit.str2Star(item.getAppSecret(), 6, 6, 6)));

        return pages;
    }

    @Cacheable(cacheNames = "mchApp", key = "#appId", unless = "#result == null")
    public MchApp getOneByMch(String mchNo, String appId){
        // 先检查布隆过滤器
        if (!BloomFilterManager.mchAppMightExist(appId)) {
            return null;
        }
        return getOne(MchApp.gw().eq(MchApp::getMchNo, mchNo).eq(MchApp::getAppId, appId));
    }

}
