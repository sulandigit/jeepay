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
package com.jeequan.jeepay.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jeequan.jeepay.core.entity.SysMessage;

import java.util.List;

/**
 * <p>
 * 系统站内消息表 服务类
 * </p>
 *
 * @author [mybatis plus generator]
 * @since 2025
 */
public interface ISysMessageService extends IService<SysMessage> {

    /**
     * 发送系统消息
     * @param message 消息对象
     * @return 是否发送成功
     * @since 2025
     */
    boolean sendMessage(SysMessage message);

    /**
     * 发送消息给指定用户
     * @param userId 用户ID
     * @param title 标题
     * @param content 内容
     * @param msgType 消息类型
     * @param sysType 系统类型
     * @return 是否发送成功
     * @since 2025
     */
    boolean sendToUser(Long userId, String title, String content, Byte msgType, String sysType);

    /**
     * 发送消息给所有用户
     * @param title 标题
     * @param content 内容
     * @param msgType 消息类型
     * @param sysType 系统类型
     * @return 是否发送成功
     * @since 2025
     */
    boolean sendToAll(String title, String content, Byte msgType, String sysType);

    /**
     * 标记消息为已读
     * @param msgId 消息ID
     * @param userId 用户ID
     * @return 是否标记成功
     * @since 2025
     */
    boolean markAsRead(Long msgId, Long userId);

    /**
     * 批量标记消息为已读
     * @param msgIds 消息ID列表
     * @param userId 用户ID
     * @return 更新数量
     * @since 2025
     */
    int batchMarkAsRead(List<Long> msgIds, Long userId);

    /**
     * 查询用户未读消息数量
     * @param userId 用户ID
     * @param sysType 系统类型
     * @return 未读消息数量
     * @since 2025
     */
    int countUnreadByUser(Long userId, String sysType);

}
