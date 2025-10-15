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

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.components.cache.CacheKeyConstants;
import com.jeequan.jeepay.components.cache.RedisCacheUtil;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.IsvInfo;
import com.jeequan.jeepay.core.entity.MchInfo;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.entity.PayWay;
import com.jeequan.jeepay.core.model.CursorPageResult;
import com.jeequan.jeepay.service.mapper.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * <p>
 * 支付订单表 服务实现类
 * </p>
 *
 * @author [mybatis plus generator]
 * @since 2021-04-27
 */
@Service
public class PayOrderService extends ServiceImpl<PayOrderMapper, PayOrder> {

    @Autowired private PayOrderMapper payOrderMapper;
    @Autowired private MchInfoMapper mchInfoMapper;
    @Autowired private IsvInfoMapper isvInfoMapper;
    @Autowired private PayWayMapper payWayMapper;
    @Autowired private PayOrderDivisionRecordMapper payOrderDivisionRecordMapper;
    @Autowired(required = false) private RedisCacheUtil redisCacheUtil;

    /** 更新订单状态  【订单生成】 --》 【支付中】 **/
    public boolean updateInit2Ing(String payOrderId, PayOrder payOrder){

        PayOrder updateRecord = new PayOrder();
        updateRecord.setState(PayOrder.STATE_ING);

        //同时更新， 未确定 --》 已确定的其他信息。  如支付接口的确认、 费率的计算。
        updateRecord.setIfCode(payOrder.getIfCode());
        updateRecord.setWayCode(payOrder.getWayCode());
        updateRecord.setMchFeeRate(payOrder.getMchFeeRate());
        updateRecord.setMchFeeAmount(payOrder.getMchFeeAmount());
        updateRecord.setChannelUser(payOrder.getChannelUser());
        updateRecord.setChannelOrderNo(payOrder.getChannelOrderNo());

        boolean result = update(updateRecord, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getPayOrderId, payOrderId).eq(PayOrder::getState, PayOrder.STATE_INIT));
        
        // 更新成功后清除缓存
        if (result) {
            PayOrder order = new PayOrder();
            order.setPayOrderId(payOrderId);
            order.setMchNo(payOrder.getMchNo());
            order.setMchOrderNo(payOrder.getMchOrderNo());
            clearOrderCache(order);
        }
        
        return result;
    }

    /** 更新订单状态  【支付中】 --》 【支付成功】 **/
    public boolean updateIng2Success(String payOrderId, String channelOrderNo, String channelUserId){

        PayOrder updateRecord = new PayOrder();
        updateRecord.setState(PayOrder.STATE_SUCCESS);
        updateRecord.setChannelOrderNo(channelOrderNo);
        updateRecord.setChannelUser(channelUserId);
        updateRecord.setSuccessTime(new Date());

        boolean result = update(updateRecord, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getPayOrderId, payOrderId).eq(PayOrder::getState, PayOrder.STATE_ING));
        
        // 更新成功后清除缓存
        if (result) {
            PayOrder order = getById(payOrderId);
            clearOrderCache(order);
        }
        
        return result;
    }

    /** 更新订单状态  【支付中】 --》 【订单关闭】 **/
    public boolean updateIng2Close(String payOrderId){

        PayOrder updateRecord = new PayOrder();
        updateRecord.setState(PayOrder.STATE_CLOSED);

        return update(updateRecord, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getPayOrderId, payOrderId).eq(PayOrder::getState, PayOrder.STATE_ING));
    }

    /** 更新订单状态  【订单生成】 --》 【订单关闭】 **/
    public boolean updateInit2Close(String payOrderId){

        PayOrder updateRecord = new PayOrder();
        updateRecord.setState(PayOrder.STATE_CLOSED);

        return update(updateRecord, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getPayOrderId, payOrderId).eq(PayOrder::getState, PayOrder.STATE_INIT));
    }


    /** 更新订单状态  【支付中】 --》 【支付失败】 **/
    public boolean updateIng2Fail(String payOrderId, String channelOrderNo, String channelUserId, String channelErrCode, String channelErrMsg){

        PayOrder updateRecord = new PayOrder();
        updateRecord.setState(PayOrder.STATE_FAIL);
        updateRecord.setErrCode(channelErrCode);
        updateRecord.setErrMsg(channelErrMsg);
        updateRecord.setChannelOrderNo(channelOrderNo);
        updateRecord.setChannelUser(channelUserId);

        return update(updateRecord, new LambdaUpdateWrapper<PayOrder>()
                .eq(PayOrder::getPayOrderId, payOrderId).eq(PayOrder::getState, PayOrder.STATE_ING));
    }


    /** 更新订单状态  【支付中】 --》 【支付成功/支付失败】 **/
    public boolean updateIng2SuccessOrFail(String payOrderId, Byte updateState, String channelOrderNo, String channelUserId, String channelErrCode, String channelErrMsg){

        if(updateState == PayOrder.STATE_ING){
            return true;
        }else if(updateState == PayOrder.STATE_SUCCESS){
            return updateIng2Success(payOrderId, channelOrderNo, channelUserId);
        }else if(updateState == PayOrder.STATE_FAIL){
            return updateIng2Fail(payOrderId, channelOrderNo, channelUserId, channelErrCode, channelErrMsg);
        }
        return false;
    }

    /** 查询商户订单 **/
    public PayOrder queryMchOrder(String mchNo, String payOrderId, String mchOrderNo){

        if(StringUtils.isNotEmpty(payOrderId)){
            return getOne(PayOrder.gw().eq(PayOrder::getMchNo, mchNo).eq(PayOrder::getPayOrderId, payOrderId));
        }else if(StringUtils.isNotEmpty(mchOrderNo)){
            return getOne(PayOrder.gw().eq(PayOrder::getMchNo, mchNo).eq(PayOrder::getMchOrderNo, mchOrderNo));
        }else{
            return null;
        }
    }

    /**
     * 三合一订单查询（优化版本）
     * 使用UNION查询替代OR条件，每个子查询都能走索引
     * @param orderId 订单号（支付订单号/商户订单号/渠道订单号）
     * @return 订单信息
     */
    public PayOrder queryByUnionOrderId(String orderId) {
        if (StringUtils.isEmpty(orderId)) {
            return null;
        }
        return payOrderMapper.selectByUnionOrderId(orderId);
    }

    /**
     * 根据支付订单号查询（带缓存）
     * 使用Redis缓存减少数据库访问，提升查询性能
     * 
     * @param payOrderId 支付订单号
     * @return 订单信息
     */
    public PayOrder getByIdWithCache(String payOrderId) {
        if (StringUtils.isEmpty(payOrderId)) {
            return null;
        }

        // 如果缓存未启用，直接查询数据库
        if (redisCacheUtil == null) {
            return getById(payOrderId);
        }

        // 生成缓存Key
        String cacheKey = CacheKeyConstants.getPayOrderKey(payOrderId);

        // 尝试从缓存获取
        PayOrder cachedOrder = redisCacheUtil.get(cacheKey, PayOrder.class);
        if (cachedOrder != null) {
            return cachedOrder;
        }

        // 判断是否为空值缓存（防止缓存穿透）
        Object nullCache = redisCacheUtil.get(cacheKey);
        if (redisCacheUtil.isNullCache(nullCache)) {
            return null;
        }

        // 缓存未命中，查询数据库
        PayOrder order = getById(payOrderId);

        if (order != null) {
            // 存入缓存，过期时间30分钟
            redisCacheUtil.set(cacheKey, order, 1800);
        } else {
            // 设置空值缓存，防止缓存穿透
            redisCacheUtil.setNull(cacheKey);
        }

        return order;
    }

    /**
     * 根据商户订单号查询（带缓存）
     * 两级缓存策略：先缓存pay_order_id映射，再缓存订单详情
     * 
     * @param mchNo 商户号
     * @param mchOrderNo 商户订单号
     * @return 订单信息
     */
    public PayOrder getByMchOrderNoWithCache(String mchNo, String mchOrderNo) {
        if (StringUtils.isEmpty(mchNo) || StringUtils.isEmpty(mchOrderNo)) {
            return null;
        }

        // 如果缓存未启用，直接查询数据库
        if (redisCacheUtil == null) {
            return getOne(PayOrder.gw().eq(PayOrder::getMchNo, mchNo).eq(PayOrder::getMchOrderNo, mchOrderNo));
        }

        // 生成商户订单映射缓存Key
        String mchOrderCacheKey = CacheKeyConstants.getMchOrderKey(mchNo, mchOrderNo);

        // 尝试从缓存获取pay_order_id
        String payOrderId = redisCacheUtil.get(mchOrderCacheKey, String.class);

        if (StringUtils.isNotEmpty(payOrderId)) {
            // 命中映射缓存，通过pay_order_id查询详情
            return getByIdWithCache(payOrderId);
        }

        // 判断是否为空值缓存
        Object nullCache = redisCacheUtil.get(mchOrderCacheKey);
        if (redisCacheUtil.isNullCache(nullCache)) {
            return null;
        }

        // 缓存未命中，查询数据库
        PayOrder order = getOne(PayOrder.gw().eq(PayOrder::getMchNo, mchNo).eq(PayOrder::getMchOrderNo, mchOrderNo));

        if (order != null) {
            // 存入两级缓存
            // 1. 缓存商户订单号映射，过期时间1小时
            redisCacheUtil.set(mchOrderCacheKey, order.getPayOrderId(), 3600);
            // 2. 缓存订单详情
            String orderCacheKey = CacheKeyConstants.getPayOrderKey(order.getPayOrderId());
            redisCacheUtil.set(orderCacheKey, order, 1800);
        } else {
            // 设置空值缓存
            redisCacheUtil.setNull(mchOrderCacheKey);
        }

        return order;
    }

    /**
     * 清除订单缓存
     * 在订单状态变更、金额变更时调用
     * 
     * @param payOrder 订单信息
     */
    public void clearOrderCache(PayOrder payOrder) {
        if (redisCacheUtil == null || payOrder == null) {
            return;
        }

        // 删除订单详情缓存
        String orderCacheKey = CacheKeyConstants.getPayOrderKey(payOrder.getPayOrderId());
        redisCacheUtil.delete(orderCacheKey);

        // 删除商户订单映射缓存
        if (StringUtils.isNotEmpty(payOrder.getMchNo()) && StringUtils.isNotEmpty(payOrder.getMchOrderNo())) {
            String mchOrderCacheKey = CacheKeyConstants.getMchOrderKey(payOrder.getMchNo(), payOrder.getMchOrderNo());
            redisCacheUtil.delete(mchOrderCacheKey);
        }
    }


    public Map payCount(String mchNo, Byte state, Byte refundState, String dayStart, String dayEnd) {
        Map param = new HashMap<>();
        if (state != null) {
            param.put("state", state);
        }
        if (refundState != null) {
            param.put("refundState", refundState);
        }
        if (StrUtil.isNotBlank(mchNo)) {
            param.put("mchNo", mchNo);
        }
        if (StrUtil.isNotBlank(dayStart)) {
            param.put("createTimeStart", dayStart);
        }
        if (StrUtil.isNotBlank(dayEnd)) {
            param.put("createTimeEnd", dayEnd);
        }
        return payOrderMapper.payCount(param);
    }

    public List<Map> payTypeCount(String mchNo, Byte state, Byte refundState, String dayStart, String dayEnd) {
        Map param = new HashMap<>();
        if (state != null) {
            param.put("state", state);
        }
        if (refundState != null) {
            param.put("refundState", refundState);
        }
        if (StrUtil.isNotBlank(mchNo)) {
            param.put("mchNo", mchNo);
        }
        if (StrUtil.isNotBlank(dayStart)) {
            param.put("createTimeStart", dayStart);
        }
        if (StrUtil.isNotBlank(dayEnd)) {
            param.put("createTimeEnd", dayEnd);
        }
        return payOrderMapper.payTypeCount(param);
    }

    /** 更新订单为 超时状态 **/
    public Integer updateOrderExpired(){

        PayOrder payOrder = new PayOrder();
        payOrder.setState(PayOrder.STATE_CLOSED);

        return baseMapper.update(payOrder,
                PayOrder.gw()
                        .in(PayOrder::getState, Arrays.asList(PayOrder.STATE_INIT, PayOrder.STATE_ING))
                        .le(PayOrder::getExpiredTime, new Date())
        );
    }

    /** 更新订单 通知状态 --> 已发送 **/
    public int updateNotifySent(String payOrderId){
        PayOrder payOrder = new PayOrder();
        payOrder.setNotifyState(CS.YES);
        payOrder.setPayOrderId(payOrderId);
        return baseMapper.updateById(payOrder);
    }

    /** 首页支付周统计 **/
    public JSONObject mainPageWeekCount(String mchNo) {
        JSONObject json = new JSONObject();
        Map dayAmount = new LinkedHashMap();
        ArrayList array = new ArrayList<>();
        BigDecimal payAmount = new BigDecimal(0);    // 当日金额
        BigDecimal payWeek  = payAmount;   // 周总收益
        String todayAmount = "0.00";    // 今日金额
        String todayPayCount = "0";    // 今日交易笔数
        String yesterdayAmount = "0.00";    // 昨日金额
        Date today = new Date();
        for(int i = 0 ; i < 7 ; i++){
            Date date = DateUtil.offsetDay(today, -i).toJdkDate();
            String dayStart = DateUtil.beginOfDay(date).toString(DatePattern.NORM_DATETIME_MINUTE_PATTERN);
            String dayEnd = DateUtil.endOfDay(date).toString(DatePattern.NORM_DATETIME_MINUTE_PATTERN);
            // 每日交易金额查询
            dayAmount = payCount(mchNo, PayOrder.STATE_SUCCESS, null, dayStart, dayEnd);
            if (dayAmount != null) {
                payAmount = new BigDecimal(dayAmount.get("payAmount").toString());
            }
            if (i == 0) {
                todayAmount = dayAmount.get("payAmount").toString();
                todayPayCount = dayAmount.get("payCount").toString();
            }
            if (i == 1) {
                yesterdayAmount = dayAmount.get("payAmount").toString();
            }
            payWeek = payWeek.add(payAmount);
            array.add(payAmount);
        }

        // 倒序排列
        Collections.reverse(array);
        json.put("dataArray", array);
        json.put("todayAmount", todayAmount);
        json.put("todayPayCount", todayPayCount);
        json.put("payWeek", payWeek);
        json.put("yesterdayAmount", yesterdayAmount);
        return json;
    }

    /** 首页统计总数量 **/
    public JSONObject mainPageNumCount(String mchNo) {
        JSONObject json = new JSONObject();
        // 商户总数
        int mchCount = mchInfoMapper.selectCount(MchInfo.gw());
        // 服务商总数
        int isvCount = isvInfoMapper.selectCount(IsvInfo.gw());
        // 总交易金额
        Map<String, String> payCountMap = payCount(mchNo, PayOrder.STATE_SUCCESS, null, null, null);
        json.put("totalMch", mchCount);
        json.put("totalIsv", isvCount);
        json.put("totalAmount", payCountMap.get("payAmount"));
        json.put("totalCount", payCountMap.get("payCount"));
        return json;
    }

    /** 首页支付统计 **/
    public List<Map> mainPagePayCount(String mchNo, String createdStart, String createdEnd) {
        Map param = new HashMap<>(); // 条件参数
        int daySpace = 6; // 默认最近七天（含当天）
        if (StringUtils.isNotEmpty(createdStart) && StringUtils.isNotEmpty(createdEnd)) {
            createdStart = createdStart + " 00:00:00";
            createdEnd = createdEnd + " 23:59:59";
            // 计算两时间间隔天数
            daySpace = Math.toIntExact(DateUtil.betweenDay(DateUtil.parseDate(createdStart), DateUtil.parseDate(createdEnd), true));
        } else {
            Date today = new Date();
            createdStart = DateUtil.formatDate(DateUtil.offsetDay(today, -daySpace)) + " 00:00:00";
            createdEnd = DateUtil.formatDate(today) + " 23:59:59";
        }

        if (StrUtil.isNotBlank(mchNo)) {
            param.put("mchNo", mchNo);
        }
        param.put("createTimeStart", createdStart);
        param.put("createTimeEnd", createdEnd);
        // 使用优化后的统计查询
        List<Map> payOrderList = payOrderMapper.selectOrderCountOptimized(param);
        List<Map> refundOrderList = payOrderMapper.selectOrderCountOptimized(param);
        // 生成前端返回参数类型
        List<Map> returnList = getReturnList(daySpace, createdEnd, payOrderList, refundOrderList);
        return returnList;
    }

    /** 首页支付类型统计 **/
    public ArrayList mainPagePayTypeCount(String mchNo, String createdStart, String createdEnd) {
        // 返回数据列
        ArrayList array = new ArrayList<>();
        if (StringUtils.isNotEmpty(createdStart) && StringUtils.isNotEmpty(createdEnd)) {
            createdStart = createdStart + " 00:00:00";
            createdEnd = createdEnd + " 23:59:59";
        }else {
            Date endDay = new Date();    // 当前日期
            Date startDay = DateUtil.lastWeek().toJdkDate(); // 一周前日期
            String end = DateUtil.formatDate(endDay);
            String start = DateUtil.formatDate(startDay);
            createdStart = start + " 00:00:00";
            createdEnd = end + " 23:59:59";
        }
        // 统计列表
        List<Map> payCountMap = payTypeCount(mchNo, PayOrder.STATE_SUCCESS, null, createdStart, createdEnd);

        // 得到所有支付方式
        Map<String, String> payWayNameMap = new HashMap<>();
        List<PayWay> payWayList = payWayMapper.selectList(PayWay.gw());
        for (PayWay payWay:payWayList) {
            payWayNameMap.put(payWay.getWayCode(), payWay.getWayName());
        }
        // 支付方式名称标注
        for (Map payCount:payCountMap) {
            if (StringUtils.isNotEmpty(payWayNameMap.get(payCount.get("wayCode")))) {
                payCount.put("typeName", payWayNameMap.get(payCount.get("wayCode")));
            }else {
                payCount.put("typeName", payCount.get("wayCode"));
            }
        }
        array.add(payCountMap);
        return array;
    }

    /** 生成首页交易统计数据类型 **/
    public List<Map> getReturnList(int daySpace, String createdStart, List<Map> payOrderList, List<Map> refundOrderList) {
        List<Map> dayList = new ArrayList<>();
        DateTime endDay = DateUtil.parseDateTime(createdStart);
        // 先判断间隔天数 根据天数设置空的list
        for (int i = 0; i <= daySpace ; i++) {
            Map<String, String> map = new HashMap<>();
            map.put("date", DateUtil.format(DateUtil.offsetDay(endDay, -i), "MM-dd"));
            dayList.add(map);
        }
        // 日期倒序排列
        Collections.reverse(dayList);

        List<Map> payListMap = new ArrayList<>(); // 收款的列
        List<Map> refundListMap = new ArrayList<>(); // 退款的列
        for (Map dayMap:dayList) {
            // 为收款列和退款列赋值默认参数【payAmount字段切记不可为string，否则前端图表解析不出来】
            Map<String, Object> payMap = new HashMap<>();
            payMap.put("date", dayMap.get("date").toString());
            payMap.put("type", "收款");
            payMap.put("payAmount", 0);

            Map<String, Object> refundMap = new HashMap<>();
            refundMap.put("date", dayMap.get("date").toString());
            refundMap.put("type", "退款");
            refundMap.put("payAmount", 0);
            for (Map payOrderMap:payOrderList) {
                if (dayMap.get("date").equals(payOrderMap.get("groupDate"))) {
                    payMap.put("payAmount", payOrderMap.get("payAmount"));
                }
            }
            payListMap.add(payMap);
            for (Map refundOrderMap:refundOrderList) {
                if (dayMap.get("date").equals(refundOrderMap.get("groupDate"))) {
                    refundMap.put("payAmount", refundOrderMap.get("refundAmount"));
                }
            }
            refundListMap.add(refundMap);
        }
        payListMap.addAll(refundListMap);
        return payListMap;
    }


    /**
    *  计算支付订单商家入账金额
    * 商家订单入账金额 （支付金额 - 手续费 - 退款金额 - 总分账金额）
    * @author terrfly
    * @site https://www.jeequan.com
    * @date 2021/8/26 16:39
    */
    public Long calMchIncomeAmount(PayOrder dbPayOrder){

        //商家订单入账金额 （支付金额 - 手续费 - 退款金额 - 总分账金额）
        Long mchIncomeAmount = dbPayOrder.getAmount() - dbPayOrder.getMchFeeAmount() - dbPayOrder.getRefundAmount();

        //减去已分账金额
        mchIncomeAmount -= payOrderDivisionRecordMapper.sumSuccessDivisionAmount(dbPayOrder.getPayOrderId());

        return mchIncomeAmount <= 0 ? 0 : mchIncomeAmount;

    }

    /**
     * 通用列表查询条件
     * @param iPage
     * @param payOrder
     * @param paramJSON
     * @param wrapper
     * @return
     */
    public IPage<PayOrder> listByPage(IPage iPage, PayOrder payOrder, JSONObject paramJSON, LambdaQueryWrapper<PayOrder> wrapper) {
        if (StringUtils.isNotEmpty(payOrder.getPayOrderId())) {
            wrapper.eq(PayOrder::getPayOrderId, payOrder.getPayOrderId());
        }
        if (StringUtils.isNotEmpty(payOrder.getMchNo())) {
            wrapper.eq(PayOrder::getMchNo, payOrder.getMchNo());
        }
        if (StringUtils.isNotEmpty(payOrder.getIsvNo())) {
            wrapper.eq(PayOrder::getIsvNo, payOrder.getIsvNo());
        }
        if (payOrder.getMchType() != null) {
            wrapper.eq(PayOrder::getMchType, payOrder.getMchType());
        }
        if (StringUtils.isNotEmpty(payOrder.getWayCode())) {
            wrapper.eq(PayOrder::getWayCode, payOrder.getWayCode());
        }
        if (StringUtils.isNotEmpty(payOrder.getMchOrderNo())) {
            wrapper.eq(PayOrder::getMchOrderNo, payOrder.getMchOrderNo());
        }
        if (payOrder.getState() != null) {
            wrapper.eq(PayOrder::getState, payOrder.getState());
        }
        if (payOrder.getNotifyState() != null) {
            wrapper.eq(PayOrder::getNotifyState, payOrder.getNotifyState());
        }
        if (StringUtils.isNotEmpty(payOrder.getAppId())) {
            wrapper.eq(PayOrder::getAppId, payOrder.getAppId());
        }
        if (payOrder.getDivisionState() != null) {
            wrapper.eq(PayOrder::getDivisionState, payOrder.getDivisionState());
        }
        if (paramJSON != null) {
            if (StringUtils.isNotEmpty(paramJSON.getString("createdStart"))) {
                wrapper.ge(PayOrder::getCreatedAt, paramJSON.getString("createdStart"));
            }
            if (StringUtils.isNotEmpty(paramJSON.getString("createdEnd"))) {
                wrapper.le(PayOrder::getCreatedAt, paramJSON.getString("createdEnd"));
            }
        }
        // 三合一订单
        if (paramJSON != null && StringUtils.isNotEmpty(paramJSON.getString("unionOrderId"))) {
            wrapper.and(wr -> {
                wr.eq(PayOrder::getPayOrderId, paramJSON.getString("unionOrderId"))
                        .or().eq(PayOrder::getMchOrderNo, paramJSON.getString("unionOrderId"))
                        .or().eq(PayOrder::getChannelOrderNo, paramJSON.getString("unionOrderId"));
            });
        }

        wrapper.orderByDesc(PayOrder::getCreatedAt);

        return page(iPage, wrapper);
    }

    /**
     * 游标分页查询支付订单
     * 解决深分页性能问题，适用于大数据量场景
     * 
     * @param payOrder 查询条件
     * @param paramJSON 扩展参数（包含cursor和pageSize）
     * @param pageSize 每页大小，默认20
     * @param cursor 游标（上一页最后一条记录的pay_order_id）
     * @return 游标分页结果
     */
    public CursorPageResult<PayOrder> listByCursor(PayOrder payOrder, JSONObject paramJSON, Integer pageSize, String cursor) {
        // 默认每页显示20条
        if (pageSize == null || pageSize <= 0 || pageSize > 100) {
            pageSize = 20;
        }
        
        // 构建查询参数
        Map<String, Object> param = new HashMap<>();
        param.put("pageSize", pageSize);
        
        if (StringUtils.isNotEmpty(cursor)) {
            param.put("cursor", cursor);
        }
        
        if (StringUtils.isNotEmpty(payOrder.getMchNo())) {
            param.put("mchNo", payOrder.getMchNo());
        }
        if (StringUtils.isNotEmpty(payOrder.getIsvNo())) {
            param.put("isvNo", payOrder.getIsvNo());
        }
        if (StringUtils.isNotEmpty(payOrder.getAppId())) {
            param.put("appId", payOrder.getAppId());
        }
        if (payOrder.getState() != null) {
            param.put("state", payOrder.getState());
        }
        if (StringUtils.isNotEmpty(payOrder.getWayCode())) {
            param.put("wayCode", payOrder.getWayCode());
        }
        if (payOrder.getNotifyState() != null) {
            param.put("notifyState", payOrder.getNotifyState());
        }
        if (payOrder.getDivisionState() != null) {
            param.put("divisionState", payOrder.getDivisionState());
        }
        
        if (paramJSON != null) {
            if (StringUtils.isNotEmpty(paramJSON.getString("createdStart"))) {
                param.put("createdStart", paramJSON.getString("createdStart"));
            }
            if (StringUtils.isNotEmpty(paramJSON.getString("createdEnd"))) {
                param.put("createdEnd", paramJSON.getString("createdEnd"));
            }
        }
        
        // 执行查询
        List<PayOrder> records = payOrderMapper.selectByCursor(param);
        
        // 构建分页结果
        CursorPageResult<PayOrder> result = CursorPageResult.of(records, pageSize);
        
        // 设置下一页游标
        if (result.isHasNext() && records.size() > 0) {
            PayOrder lastRecord = records.get(records.size() - 1);
            result.setNextCursor(lastRecord.getPayOrderId());
        }
        
        return result;
    }
}
