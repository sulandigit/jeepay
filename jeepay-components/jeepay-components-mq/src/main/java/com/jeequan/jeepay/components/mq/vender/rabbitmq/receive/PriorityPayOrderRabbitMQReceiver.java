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
package com.jeequan.jeepay.components.mq.vender.rabbitmq.receive;

import com.jeequan.jeepay.components.mq.constant.MQVenderCS;
import com.jeequan.jeepay.components.mq.model.PriorityPayOrderMQ;
import com.jeequan.jeepay.components.mq.vender.IMQMsgReceiver;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * rabbitMQ消息接收器：仅在vender=rabbitMQ时 && 项目实现IMQReceiver接口时 进行实例化
 * 业务：  支付订单处理 - 优先级队列示例
 *
 * 使用说明：
 * 1. 此接收器演示如何接收优先级队列的消息
 * 2. 消息会按照优先级顺序被消费（优先级高的先消费）
 * 3. 在实际使用时，需要在对应的业务模块中实现IMQReceiver接口
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2021/7/22 17:06
 */
@Component
@ConditionalOnProperty(name = MQVenderCS.YML_VENDER_KEY, havingValue = MQVenderCS.RABBIT_MQ)
@ConditionalOnBean(PriorityPayOrderMQ.IMQReceiver.class)
public class PriorityPayOrderRabbitMQReceiver implements IMQMsgReceiver {

    @Autowired
    private PriorityPayOrderMQ.IMQReceiver mqReceiver;

    /** 
     * 接收 【 queue 】 类型的消息
     * 
     * 说明：
     * - @RabbitListener 注解会自动监听指定队列
     * - 队列已在RabbitMQConfig中自动创建并配置了优先级参数
     * - 消息会按照优先级从高到低的顺序被消费
     **/
    @Override
    @RabbitListener(queues = PriorityPayOrderMQ.MQ_NAME)
    public void receiveMsg(String msg){
        mqReceiver.receive(PriorityPayOrderMQ.parse(msg));
    }

}
