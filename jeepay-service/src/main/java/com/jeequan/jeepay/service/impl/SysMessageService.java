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

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.entity.SysMessage;
import com.jeequan.jeepay.core.service.ISysMessageService;
import com.jeequan.jeepay.service.mapper.SysMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 系统站内消息表 服务实现类
 * </p>
 *
 * @author [mybatis plus generator]
 * @since 2025
 */
@Slf4j
@Service
public class SysMessageService extends ServiceImpl<SysMessageMapper, SysMessage> implements ISysMessageService {

    @Autowired
    private SysMessageMapper sysMessageMapper;

    /**
     * 发送系统消息
     * @param message 消息对象
     * @return 是否发送成功
     * @since 2025
     */
    @Override
    public boolean sendMessage(SysMessage message) {
        try {
            message.setState(SysMessage.STATE_UNREAD);
            message.setCreatedAt(new Date());
            message.setUpdatedAt(new Date());
            return save(message);
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return false;
        }
    }

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
    @Override
    public boolean sendToUser(Long userId, String title, String content, Byte msgType, String sysType) {
        SysMessage message = new SysMessage();
        message.setReceiverUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setMsgType(msgType);
        message.setSysType(sysType);
        message.setPushType(SysMessage.PUSH_TYPE_SPECIFIC);
        return sendMessage(message);
    }

    /**
     * 发送消息给所有用户
     * @param title 标题
     * @param content 内容
     * @param msgType 消息类型
     * @param sysType 系统类型
     * @return 是否发送成功
     * @since 2025
     */
    @Override
    public boolean sendToAll(String title, String content, Byte msgType, String sysType) {
        SysMessage message = new SysMessage();
        message.setTitle(title);
        message.setContent(content);
        message.setMsgType(msgType);
        message.setSysType(sysType);
        message.setPushType(SysMessage.PUSH_TYPE_ALL);
        return sendMessage(message);
    }

    /**
     * 标记消息为已读
     * @param msgId 消息ID
     * @param userId 用户ID
     * @return 是否标记成功
     * @since 2025
     */
    @Override
    public boolean markAsRead(Long msgId, Long userId) {
        SysMessage message = getById(msgId);
        if (message == null) {
            return false;
        }
        
        // 验证消息所属用户
        if (!message.getReceiverUserId().equals(userId)) {
            log.warn("用户{}尝试标记非本人消息{}为已读", userId, msgId);
            return false;
        }
        
        message.setState(SysMessage.STATE_READ);
        message.setReadTime(new Date());
        message.setUpdatedAt(new Date());
        return updateById(message);
    }

    /**
     * 批量标记消息为已读
     * @param msgIds 消息ID列表
     * @param userId 用户ID
     * @return 更新数量
     * @since 2025
     */
    @Override
    public int batchMarkAsRead(List<Long> msgIds, Long userId) {
        if (msgIds == null || msgIds.isEmpty()) {
            return 0;
        }
        return sysMessageMapper.batchMarkRead(msgIds, userId);
    }

    /**
     * 查询用户未读消息数量
     * @param userId 用户ID
     * @param sysType 系统类型
     * @return 未读消息数量
     * @since 2025
     */
    @Override
    public int countUnreadByUser(Long userId, String sysType) {
        return sysMessageMapper.countUnreadByUser(userId, sysType);
    }

}
