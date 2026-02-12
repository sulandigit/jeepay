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

import com.jeequan.jeepay.components.mq.model.PayOrderReissueMQ;
import com.jeequan.jeepay.components.mq.vender.IMQSender;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.pay.rqrs.msg.ChannelRetMsg;
import com.jeequan.jeepay.pay.service.ChannelOrderReissueService;
import com.jeequan.jeepay.service.impl.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Payment Order Reissue (Query) MQ Receiver
 * 支付订单补单（查询）MQ消息接收器
 * <p>
 * Receives and processes MQ messages for payment order reissue/query operations.
 * This is typically used for payment interfaces without callbacks, such as WeChat barcode payment.
 * Implements polling mechanism with maximum 6 query attempts, 5 seconds interval.
 * 接收并处理支付订单补单/查询操作的MQ消息。
 * 通常用于没有回调的接口，比如微信的条码支付。
 * 实现了轮询机制，最多6次查询，间隔5秒。
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2021/7/27 9:23
 */
@Slf4j
@Component
public class PayOrderReissueMQReceiver implements PayOrderReissueMQ.IMQReceiver {

    /** MQ sender for delayed query retry / MQ发送器，用于延迟查询重试 */
    @Autowired
    private IMQSender mqSender;
    
    /** Payment order service / 支付订单服务 */
    @Autowired
    private PayOrderService payOrderService;
    
    /** Channel order reissue service / 渠道订单补单服务 */
    @Autowired
    private ChannelOrderReissueService channelOrderReissueService;

    /**
     * Receive and process payment order reissue/query MQ message
     * 接收并处理支付订单补单/查询MQ消息
     * <p>
     * Query strategy:
     * - Maximum 6 query attempts
     * - 5 seconds delay between each attempt
     * - If order still in WAITING state after 6 attempts, TODO: call [cancel order] interface
     * 查询策略：
     * - 最多6次查询尝试
     * - 每次尝试间隔5秒
     * - 6次后订单仍处于等待状态，TODO：调用【撤销订单】接口
     *
     * @param payload message payload containing:
     *                - payOrderId: payment order ID / 支付订单ID
     *                - count: current query attempt count / 当前查询尝试次数
     */
    @Override
    public void receive(PayOrderReissueMQ.MsgPayload payload) {
        try {
            String payOrderId = payload.getPayOrderId();
            int currentCount = payload.getCount();
            log.info("接收轮询查单通知MQ, payOrderId={}, count={}", payOrderId, currentCount);
            currentCount++ ;

            // Query payment order / 查询支付订单
            PayOrder payOrder = payOrderService.getById(payOrderId);
            if(payOrder == null) {
                log.warn("查询支付订单为空,payOrderId={}", payOrderId);
                return;
            }

            // Check if order is still in processing state / 检查订单是否仍在处理中
            if(payOrder.getState() != PayOrder.STATE_ING) {
                log.warn("订单状态不是支付中,不需查询渠道.payOrderId={}", payOrderId);
                return;
            }

            // Query channel order status / 查询渠道订单状态
            ChannelRetMsg channelRetMsg = channelOrderReissueService.processPayOrder(payOrder);

            // Returns null may indicate interface error, need to poll again / 返回null 可能为接口报错等， 需要再次轮询
            if(channelRetMsg == null || channelRetMsg.getChannelState() == null || channelRetMsg.getChannelState().equals(ChannelRetMsg.ChannelState.WAITING)){

                // Maximum 6 query attempts / 最多查询6次
                if(currentCount <= 6){
                    mqSender.send(PayOrderReissueMQ.build(payOrderId, currentCount), 5); // Delay 5s before next query / 延迟5s再次查询
                }else{

                    // TODO: Call [cancel order] interface / 调用【撤销订单】接口

                }

            }else{ // Other states, no need to poll again / 其他状态， 不需要再次轮询。
            }
        }catch (Exception e) {
            log.error(e.getMessage());
            return;
        }
    }
}
