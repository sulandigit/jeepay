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
package com.jeequan.jeepay.components.mq.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.components.mq.constant.MQSendTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
*
* 定义MQ消息格式 - 支持优先级队列示例
* 业务场景： [ 支付订单处理 - 优先级队列示例 ]
* 
* 使用说明：
* 1. 重写getMaxPriority()方法，返回队列最大优先级（推荐1-10）
* 2. 在构建消息时设置messagePriority属性，指定具体消息的优先级
* 3. 优先级数值越大，消息处理优先级越高
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/7/22 15:25
*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriorityPayOrderMQ extends AbstractMQ {

    /** 【！重要配置项！】 定义MQ名称 **/
    public static final String MQ_NAME = "QUEUE_PRIORITY_PAY_ORDER";

    /** 内置msg 消息体定义 **/
    private MsgPayload payload;

    /** 消息优先级，数值越大优先级越高 **/
    private int messagePriority;

    /**  【！重要配置项！】 定义Msg消息载体 **/
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MsgPayload {

        /** 支付订单号 **/
        private String payOrderId;

        /** 订单金额 **/
        private Long amount;

        /** 业务类型，用于区分优先级 (1:普通订单 2:VIP订单 3:紧急订单) **/
        private Integer bizType;

    }

    @Override
    public String getMQName() {
        return MQ_NAME;
    }

    /**  【！重要配置项！】 **/
    @Override
    public MQSendTypeEnum getMQType(){
        return MQSendTypeEnum.QUEUE;  // QUEUE - 点对点 、 BROADCAST - 广播模式
    }

    @Override
    public String toMessage() {
        return JSONObject.toJSONString(payload);
    }

    /**
     * 【！重要配置项！】 设置队列最大优先级
     * 返回值范围: 1-255 (推荐使用1-10)
     * 注意: 
     * 1. 优先级越高，对RabbitMQ性能影响越大
     * 2. 生产环境推荐使用较小的优先级范围(如1-10)
     * 3. 返回0表示不启用优先级队列
     */
    @Override
    public int getMaxPriority() {
        return 10;  // 设置队列支持0-10的优先级
    }

    /**
     * 获取当前消息的优先级
     * 优先级范围: 0 到 getMaxPriority()
     * 数值越大优先级越高
     */
    @Override
    public int getMessagePriority() {
        return this.messagePriority;
    }

    /**  【！重要配置项！】 构造MQModel , 一般用于发送MQ时 **/
    public static PriorityPayOrderMQ build(String payOrderId, Long amount, Integer bizType){
        // 根据业务类型自动设置优先级
        int priority = calculatePriority(bizType);
        return new PriorityPayOrderMQ(new MsgPayload(payOrderId, amount, bizType), priority);
    }

    /**  构造MQModel - 手动指定优先级 **/
    public static PriorityPayOrderMQ buildWithPriority(String payOrderId, Long amount, Integer bizType, int priority){
        return new PriorityPayOrderMQ(new MsgPayload(payOrderId, amount, bizType), priority);
    }

    /** 根据业务类型计算优先级 **/
    private static int calculatePriority(Integer bizType) {
        if (bizType == null) {
            return 0;  // 默认优先级
        }
        switch (bizType) {
            case 3:  // 紧急订单
                return 10;
            case 2:  // VIP订单
                return 5;
            case 1:  // 普通订单
            default:
                return 1;
        }
    }

    /** 解析MQ消息， 一般用于接收MQ消息时 **/
    public static MsgPayload parse(String msg){
        return JSON.parseObject(msg, MsgPayload.class);
    }

    /** 定义 IMQReceiver 接口： 项目实现该接口则可接收到对应的业务消息  **/
    public interface IMQReceiver{
        void receive(MsgPayload payload);
    }

}
