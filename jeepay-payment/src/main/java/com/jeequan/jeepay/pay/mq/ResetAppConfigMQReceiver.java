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

import com.jeequan.jeepay.components.mq.model.ResetAppConfigMQ;
import com.jeequan.jeepay.service.impl.SysConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Reset Application Configuration MQ Receiver
 * 重置应用配置MQ消息接收器
 * <p>
 * Receives and processes MQ messages for resetting system configuration parameters.
 * When configuration is updated in database, this receiver reloads the configuration
 * from database to ensure all service instances have the latest configuration.
 * 接收并处理重置系统配置参数的MQ消息。
 * 当数据库中的配置更新时，该接收器从数据库重新加载配置，
 * 以确保所有服务实例都有最新的配置。
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2021/7/27 9:23
 */
@Slf4j
@Component
public class ResetAppConfigMQReceiver implements ResetAppConfigMQ.IMQReceiver {

    /** System configuration service / 系统配置服务 */
    @Autowired
    private SysConfigService sysConfigService;

    /**
     * Receive and process system configuration reset MQ message
     * 接收并处理系统配置重置MQ消息
     * <p>
     * Reloads system configuration from database based on the specified group key.
     * 根据指定的组键从数据库重新加载系统配置。
     *
     * @param payload message payload containing groupKey / 消息载荷，包含配置组键
     */
    @Override
    public void receive(ResetAppConfigMQ.MsgPayload payload) {

        log.info("成功接收更新系统配置的订阅通知, msg={}", payload);
        // Reinitialize database configuration by group key / 根据组键重新初始化数据库配置
        sysConfigService.initDBConfig(payload.getGroupKey());
        log.info("系统配置静态属性已重置");
    }
}
