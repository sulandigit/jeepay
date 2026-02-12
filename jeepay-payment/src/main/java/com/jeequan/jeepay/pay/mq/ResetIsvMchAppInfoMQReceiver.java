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
package com.jeequan.jeepay.pay.mq;

import com.jeequan.jeepay.components.mq.model.ResetIsvMchAppInfoConfigMQ;
import com.jeequan.jeepay.pay.service.ConfigContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reset ISV/Merchant/App Information MQ Receiver
 * 重置服务商/商户/应用信息MQ消息接收器
 * <p>
 * Receives and processes MQ messages for updating ISV (Independent Software Vendor),
 * merchant, and merchant application configuration information.
 * This ensures configuration changes are synchronized across all service instances.
 * 接收并处理更新服务商、商户和商户应用配置信息的MQ消息。
 * 这确保配置更改在所有服务实例中同步。
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2021/7/27 9:23
 */
@Slf4j
@Component
public class ResetIsvMchAppInfoMQReceiver implements ResetIsvMchAppInfoConfigMQ.IMQReceiver {

    /** Configuration context service / 配置上下文服务 */
    @Autowired
    private ConfigContextService configContextService;

    /**
     * Receive and process ISV/Merchant/App configuration reset MQ message
     * 接收并处理ISV/商户/应用配置重置MQ消息
     * <p>
     * Routes the reset request based on reset type:
     * - RESET_TYPE_ISV_INFO: Reset ISV configuration
     * - RESET_TYPE_MCH_INFO: Reset merchant configuration
     * - RESET_TYPE_MCH_APP: Reset merchant application configuration
     * 根据重置类型路由重置请求：
     * - RESET_TYPE_ISV_INFO: 重置服务商配置
     * - RESET_TYPE_MCH_INFO: 重置商户配置
     * - RESET_TYPE_MCH_APP: 重置商户应用配置
     *
     * @param payload message payload containing reset type and related IDs / 消息载荷，包含重置类型和相关ID
     */
    @Override
    public void receive(ResetIsvMchAppInfoConfigMQ.MsgPayload payload) {

        if(payload.getResetType() == ResetIsvMchAppInfoConfigMQ.RESET_TYPE_ISV_INFO){
            this.modifyIsvInfo(payload.getIsvNo());
        }else if(payload.getResetType() == ResetIsvMchAppInfoConfigMQ.RESET_TYPE_MCH_INFO){
            this.modifyMchInfo(payload.getMchNo());
        }else if(payload.getResetType() == ResetIsvMchAppInfoConfigMQ.RESET_TYPE_MCH_APP){
            this.modifyMchApp(payload.getMchNo(), payload.getAppId());
        }

    }

    /**
     * Reset merchant configuration information
     * 重置商户配置信息
     *
     * @param mchNo merchant number / 商户编号
     */
    private void modifyMchInfo(String mchNo) {
        log.info("成功接收 [商户配置信息] 的消息, msg={}", mchNo);
        configContextService.initMchInfoConfigContext(mchNo);
        log.info(" [商户配置信息] 已重置");
    }

    /**
     * Reset merchant application payment parameter configuration information
     * 重置商户应用支付参数配置信息
     *
     * @param mchNo merchant number / 商户编号
     * @param appId application ID / 应用ID
     */
    private void modifyMchApp(String mchNo, String appId) {
        log.info("成功接收 [商户应用支付参数配置信息] 的消息, mchNo={}, appId={}", mchNo, appId);
        configContextService.initMchAppConfigContext(mchNo, appId);
        log.info(" [商户应用支付参数配置信息] 已重置");
    }

    /**
     * Reset ISV (Independent Software Vendor) configuration information
     * 重置服务商配置信息
     *
     * @param isvNo ISV number / 服务商编号
     */
    private void modifyIsvInfo(String isvNo) {
        log.info("成功接收 [ISV信息] 重置, msg={}", isvNo);
        configContextService.initIsvConfigContext(isvNo);
        log.info("[ISV信息] 已重置");
    }

}
