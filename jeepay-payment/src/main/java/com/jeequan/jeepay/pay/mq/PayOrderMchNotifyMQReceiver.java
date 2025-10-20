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

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.jeequan.jeepay.components.mq.model.PayOrderMchNotifyMQ;
import com.jeequan.jeepay.components.mq.vender.IMQSender;
import com.jeequan.jeepay.core.entity.MchNotifyRecord;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.service.impl.MchNotifyRecordService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Payment Order Merchant Notification MQ Receiver
 * 支付订单商户通知MQ消息接收器
 * <p>
 * Receives and processes MQ messages for notifying merchants about payment order status.
 * Implements retry logic with exponential backoff (0, 30, 60, 90, 120, 150 seconds).
 * Maximum retry attempts: 6 times.
 * 接收并处理向商户通知支付订单状态的MQ消息。
 * 实现了指数退避的重试逻辑（0、30、60、90、120、150秒）。
 * 最大重试次数：6次。
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2021/7/27 9:23
 */
@Slf4j
@Component
public class PayOrderMchNotifyMQReceiver implements PayOrderMchNotifyMQ.IMQReceiver {

    /** Payment order service / 支付订单服务 */
    @Autowired
    private PayOrderService payOrderService;
    
    /** Merchant notification record service / 商户通知记录服务 */
    @Autowired
    private MchNotifyRecordService mchNotifyRecordService;
    
    /** MQ sender for delayed retry / MQ发送器，用于延迟重试 */
    @Autowired
    private IMQSender mqSender;

    /**
     * Receive and process merchant notification MQ message
     * 接收并处理商户通知MQ消息
     * <p>
     * Notification retry strategy:
     * Attempt 1: Immediate (0s)
     * Attempt 2: After 30s
     * Attempt 3: After 60s
     * Attempt 4: After 90s
     * Attempt 5: After 120s
     * Attempt 6: After 150s
     * 通知重试策略：
     * 第1次：立即(0秒)
     * 第2次：30秒后
     * 第3次：60秒后
     * 第4次：90秒后
     * 第5次：120秒后
     * 第6次：150秒后
     *
     * @param payload message payload containing notifyId / 消息载荷，包含通知ID
     */
    @Override
    public void receive(PayOrderMchNotifyMQ.MsgPayload payload) {

        try {
            log.info("接收商户通知MQ, msg={}", payload.toString());

            Long notifyId = payload.getNotifyId();
            // Query notification record / 查询通知记录
            MchNotifyRecord record = mchNotifyRecordService.getById(notifyId);
            if(record == null || record.getState() != MchNotifyRecord.STATE_ING){
                log.info("查询通知记录不存在或状态不是通知中");
                return;
            }
            if( record.getNotifyCount() >= record.getNotifyCountLimit() ){
                log.info("已达到最大发送次数");
                return;
            }

            // 1. Send notification (maximum 6 attempts) / 发送通知（最多6次）
            Integer currentCount = record.getNotifyCount() + 1;

            String notifyUrl = record.getNotifyUrl();
            String res = "";
            try {
                // res = HttpUtil.createPost(notifyUrl).timeout(20000).execute().body();

                // Parse notification URL / 解析通知URL
                int pathEndPos = notifyUrl.indexOf('?');
                if (pathEndPos <= -1) {
                    log.error("通知地址错误，参数为空，notifyUrl：{}", notifyUrl);
                    throw new BizException("通知地址错误");
                }

                // Send HTTP POST request with 20s timeout / 发送HTTP POST请求，超时20秒
                res = HttpUtil.post(StrUtil.subPre(notifyUrl, pathEndPos), StrUtil.subSuf(notifyUrl, pathEndPos + 1), 20000);
            } catch (Exception e) {
                log.error("http error", e);
                res = "连接["+ UrlBuilder.of(notifyUrl).getHost() +"]异常:【" + e.getMessage() + "】";
            }

            // For payment order & first notification: update to notified status / 支付订单 & 第一次通知: 更新为已通知
            if(currentCount == 1 && MchNotifyRecord.TYPE_PAY_ORDER == record.getOrderType()){
                payOrderService.updateNotifySent(record.getOrderId());
            }

            // Notification successful / 通知成功
            if("SUCCESS".equalsIgnoreCase(res)){
                mchNotifyRecordService.updateNotifyResult(notifyId, MchNotifyRecord.STATE_SUCCESS, res);
                return;
            }

            // If notification count >= max limit, update result as failed, stop retrying / 通知次数 >= 最大通知次数时， 更新响应结果为异常， 不在继续延迟发送消息
            if( currentCount >= record.getNotifyCountLimit() ){
                mchNotifyRecordService.updateNotifyResult(notifyId, MchNotifyRecord.STATE_FAIL, res);
                return;
            }

            // Continue sending MQ with delay / 继续发送MQ 延迟发送
            mchNotifyRecordService.updateNotifyResult(notifyId, MchNotifyRecord.STATE_ING, res);
            // Notification delay sequence (in seconds) / 通知延时次数
            //        1   2  3  4   5   6
            //        0  30 60 90 120 150
            mqSender.send(PayOrderMchNotifyMQ.build(notifyId), currentCount * 30);

            return;
        }catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }
    }
}
