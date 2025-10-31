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
import com.jeequan.jeepay.core.entity.*;
import com.jeequan.jeepay.core.utils.JeepayKit;
import com.jeequan.jeepay.service.impl.TemplateParseService;
import com.jeequan.jeepay.service.impl.VariableExtractService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知消息构建器
 * 基于模板生成商户通知消息内容
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@Slf4j
@Service
public class NotifyMessageBuilder {

    @Autowired
    private TemplateCacheManager templateCacheManager;

    @Autowired
    private TemplateParseService templateParseService;

    @Autowired
    private VariableExtractService variableExtractService;

    /**
     * 构建支付订单通知消息
     *
     * @param payOrder 支付订单对象
     * @param appSecret 应用密钥
     * @return 通知消息字符串
     */
    public String buildPayOrderNotify(PayOrder payOrder, String appSecret) {
        if (payOrder == null) {
            log.error("构建支付通知失败: payOrder为空");
            return null;
        }

        try {
            // 1. 获取模板
            MsgTemplateVersion template = templateCacheManager.getActiveTemplateByType(MsgTemplate.TYPE_PAY_NOTIFY);
            if (template == null) {
                log.warn("未找到支付通知模板，使用默认格式");
                return buildDefaultPayOrderNotify(payOrder, appSecret);
            }

            // 2. 提取变量值
            Map<String, Object> variables = variableExtractService.extractFromPayOrder(payOrder);

            // 3. 添加系统时间
            variables.put("req_time", new Date());

            // 4. 生成签名(在替换变量之前，因为签名需要原始参数)
            Map<String, Object> signParams = new HashMap<>(variables);
            signParams.remove("sign");  // 移除sign字段本身
            String sign = generateSign(signParams, appSecret);
            variables.put("sign", sign);

            // 5. 解析模板
            String notifyContent = templateParseService.parseTemplate(template.getTemplateContent(), variables);

            log.info("构建支付通知成功: payOrderId={}", payOrder.getPayOrderId());
            return notifyContent;

        } catch (Exception e) {
            log.error("构建支付通知异常: payOrderId={}", payOrder.getPayOrderId(), e);
            // 异常时使用默认格式
            return buildDefaultPayOrderNotify(payOrder, appSecret);
        }
    }

    /**
     * 构建退款订单通知消息
     *
     * @param refundOrder 退款订单对象
     * @param appSecret 应用密钥
     * @return 通知消息字符串
     */
    public String buildRefundOrderNotify(RefundOrder refundOrder, String appSecret) {
        if (refundOrder == null) {
            log.error("构建退款通知失败: refundOrder为空");
            return null;
        }

        try {
            // 1. 获取模板
            MsgTemplateVersion template = templateCacheManager.getActiveTemplateByType(MsgTemplate.TYPE_REFUND_NOTIFY);
            if (template == null) {
                log.warn("未找到退款通知模板，使用默认格式");
                return buildDefaultRefundOrderNotify(refundOrder, appSecret);
            }

            // 2. 提取变量值
            Map<String, Object> variables = variableExtractService.extractFromRefundOrder(refundOrder);

            // 3. 添加系统时间
            variables.put("req_time", new Date());

            // 4. 生成签名
            Map<String, Object> signParams = new HashMap<>(variables);
            signParams.remove("sign");
            String sign = generateSign(signParams, appSecret);
            variables.put("sign", sign);

            // 5. 解析模板
            String notifyContent = templateParseService.parseTemplate(template.getTemplateContent(), variables);

            log.info("构建退款通知成功: refundOrderId={}", refundOrder.getRefundOrderId());
            return notifyContent;

        } catch (Exception e) {
            log.error("构建退款通知异常: refundOrderId={}", refundOrder.getRefundOrderId(), e);
            return buildDefaultRefundOrderNotify(refundOrder, appSecret);
        }
    }

    /**
     * 构建转账订单通知消息
     *
     * @param transferOrder 转账订单对象
     * @param appSecret 应用密钥
     * @return 通知消息字符串
     */
    public String buildTransferOrderNotify(TransferOrder transferOrder, String appSecret) {
        if (transferOrder == null) {
            log.error("构建转账通知失败: transferOrder为空");
            return null;
        }

        try {
            // 1. 获取模板
            MsgTemplateVersion template = templateCacheManager.getActiveTemplateByType(MsgTemplate.TYPE_TRANSFER_NOTIFY);
            if (template == null) {
                log.warn("未找到转账通知模板，使用默认格式");
                return buildDefaultTransferOrderNotify(transferOrder, appSecret);
            }

            // 2. 提取变量值
            Map<String, Object> variables = variableExtractService.extractFromTransferOrder(transferOrder);

            // 3. 添加系统时间
            variables.put("req_time", new Date());

            // 4. 生成签名
            Map<String, Object> signParams = new HashMap<>(variables);
            signParams.remove("sign");
            String sign = generateSign(signParams, appSecret);
            variables.put("sign", sign);

            // 5. 解析模板
            String notifyContent = templateParseService.parseTemplate(template.getTemplateContent(), variables);

            log.info("构建转账通知成功: transferId={}", transferOrder.getTransferId());
            return notifyContent;

        } catch (Exception e) {
            log.error("构建转账通知异常: transferId={}", transferOrder.getTransferId(), e);
            return buildDefaultTransferOrderNotify(transferOrder, appSecret);
        }
    }

    /**
     * 生成签名
     *
     * @param params 参数Map
     * @param appSecret 应用密钥
     * @return 签名字符串
     */
    private String generateSign(Map<String, Object> params, String appSecret) {
        if (params == null || StringUtils.isBlank(appSecret)) {
            return "";
        }

        try {
            // 将Object转为String
            Map<String, String> strParams = new HashMap<>();
            params.forEach((key, value) -> {
                if (value != null) {
                    strParams.put(key, value.toString());
                }
            });

            // 使用Jeepay工具类生成签名
            return JeepayKit.getSign(strParams, appSecret);
        } catch (Exception e) {
            log.error("生成签名失败", e);
            return "";
        }
    }

    /**
     * 构建默认格式的支付通知（兜底方案）
     */
    private String buildDefaultPayOrderNotify(PayOrder payOrder, String appSecret) {
        // TODO: 实现默认通知格式
        return "";
    }

    /**
     * 构建默认格式的退款通知（兜底方案）
     */
    private String buildDefaultRefundOrderNotify(RefundOrder refundOrder, String appSecret) {
        // TODO: 实现默认通知格式
        return "";
    }

    /**
     * 构建默认格式的转账通知（兜底方案）
     */
    private String buildDefaultTransferOrderNotify(TransferOrder transferOrder, String appSecret) {
        // TODO: 实现默认通知格式
        return "";
    }
}
