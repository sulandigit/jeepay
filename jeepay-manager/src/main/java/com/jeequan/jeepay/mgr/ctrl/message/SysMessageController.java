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
package com.jeequan.jeepay.mgr.ctrl.message;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.SysMessage;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.service.ISysMessageService;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.mgr.websocket.MessageWebSocketServer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 站内消息管理
 * 
 * @author [mybatis plus generator]
 * @since 2025
 */
@Api(tags = "系统管理（站内消息）")
@RestController
@RequestMapping("/api/sysMessages")
public class SysMessageController extends CommonCtrl {

    @Autowired
    private ISysMessageService sysMessageService;

    /**
     * 获取消息列表
     * @return 消息列表
     * @since 2025
     */
    @ApiOperation("消息列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "pageNumber", value = "分页页码", dataType = "int", defaultValue = "1"),
            @ApiImplicitParam(name = "pageSize", value = "分页条数", dataType = "int", defaultValue = "20"),
            @ApiImplicitParam(name = "title", value = "消息标题"),
            @ApiImplicitParam(name = "msgType", value = "消息类型", dataType = "Byte"),
            @ApiImplicitParam(name = "state", value = "消息状态", dataType = "Byte")
    })
    @PreAuthorize("hasAuthority('ENT_MESSAGE_LIST')")
    @GetMapping("")
    public ApiPageRes<SysMessage> list() {
        SysMessage queryObject = getObject(SysMessage.class);
        
        LambdaQueryWrapper<SysMessage> condition = SysMessage.gw();
        condition.eq(SysMessage::getSysType, CS.SYS_TYPE.MGR);
        
        // 查询当前用户的消息
        Long currentUserId = getCurrentUser().getSysUser().getSysUserId();
        condition.and(wrapper -> wrapper
                .eq(SysMessage::getReceiverUserId, currentUserId)
                .or()
                .eq(SysMessage::getPushType, SysMessage.PUSH_TYPE_ALL)
        );
        
        if (StringUtils.isNotEmpty(queryObject.getTitle())) {
            condition.like(SysMessage::getTitle, queryObject.getTitle());
        }
        
        if (queryObject.getMsgType() != null) {
            condition.eq(SysMessage::getMsgType, queryObject.getMsgType());
        }
        
        if (queryObject.getState() != null) {
            condition.eq(SysMessage::getState, queryObject.getState());
        }
        
        condition.orderByDesc(SysMessage::getCreatedAt);
        
        IPage<SysMessage> pages = sysMessageService.page(getIPage(), condition);
        return ApiPageRes.pages(pages);
    }

    /**
     * 获取未读消息数量
     * @return 未读消息数量
     * @since 2025
     */
    @ApiOperation("未读消息数量")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header")
    })
    @GetMapping("/unreadCount")
    public ApiRes unreadCount() {
        Long currentUserId = getCurrentUser().getSysUser().getSysUserId();
        int count = sysMessageService.countUnreadByUser(currentUserId, CS.SYS_TYPE.MGR);
        return ApiRes.ok(count);
    }

    /**
     * 消息详情
     * @param msgId 消息ID
     * @return 消息详情
     * @since 2025
     */
    @ApiOperation("消息详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "msgId", value = "消息ID", required = true, dataType = "Long")
    })
    @GetMapping("/{msgId}")
    public ApiRes detail(@PathVariable("msgId") Long msgId) {
        SysMessage message = sysMessageService.getById(msgId);
        return ApiRes.ok(message);
    }

    /**
     * 发送消息
     * @return 操作结果
     * @since 2025
     */
    @ApiOperation("发送消息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "title", value = "消息标题", required = true),
            @ApiImplicitParam(name = "content", value = "消息内容", required = true),
            @ApiImplicitParam(name = "msgType", value = "消息类型", required = true, dataType = "Byte"),
            @ApiImplicitParam(name = "pushType", value = "推送方式: 1-全体推送, 2-指定用户推送", required = true, dataType = "Byte"),
            @ApiImplicitParam(name = "receiverUserId", value = "接收用户ID（指定用户时必填）", dataType = "Long"),
            @ApiImplicitParam(name = "receiverUsername", value = "接收用户名（指定用户时必填）")
    })
    @PreAuthorize("hasAuthority('ENT_MESSAGE_SEND')")
    @MethodLog(remark = "发送站内消息")
    @PostMapping("")
    public ApiRes send() {
        SysMessage message = getObject(SysMessage.class);
        
        // 设置发送者信息
        message.setSenderUserId(getCurrentUser().getSysUser().getSysUserId());
        message.setSenderUsername(getCurrentUser().getSysUser().getRealname());
        message.setSysType(CS.SYS_TYPE.MGR);
        
        // 保存消息到数据库
        boolean result = sysMessageService.sendMessage(message);
        
        if (result) {
            // 通过WebSocket实时推送消息
            JSONObject wsMessage = new JSONObject();
            wsMessage.put("type", "message");
            wsMessage.put("msgId", message.getMsgId());
            wsMessage.put("title", message.getTitle());
            wsMessage.put("content", message.getContent());
            wsMessage.put("msgType", message.getMsgType());
            wsMessage.put("createdAt", message.getCreatedAt());
            
            if (message.getPushType() == SysMessage.PUSH_TYPE_ALL) {
                // 群发消息
                MessageWebSocketServer.sendMessageToAll(wsMessage);
            } else if (message.getPushType() == SysMessage.PUSH_TYPE_SPECIFIC) {
                // 发送给指定用户
                MessageWebSocketServer.sendMessageToUser(
                    message.getReceiverUserId().toString(), 
                    wsMessage
                );
            }
        }
        
        return result ? ApiRes.ok() : ApiRes.fail(ApiRes.CODE_ERROR, "发送失败");
    }

    /**
     * 标记消息为已读
     * @param msgId 消息ID
     * @return 操作结果
     * @since 2025
     */
    @ApiOperation("标记消息为已读")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "msgId", value = "消息ID", required = true, dataType = "Long")
    })
    @MethodLog(remark = "标记消息为已读")
    @PutMapping("/{msgId}/read")
    public ApiRes markAsRead(@PathVariable("msgId") Long msgId) {
        Long currentUserId = getCurrentUser().getSysUser().getSysUserId();
        boolean result = sysMessageService.markAsRead(msgId, currentUserId);
        return result ? ApiRes.ok() : ApiRes.fail(ApiRes.CODE_ERROR, "操作失败");
    }

    /**
     * 批量标记消息为已读
     * @return 操作结果
     * @since 2025
     */
    @ApiOperation("批量标记消息为已读")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "msgIds", value = "消息ID列表（逗号分隔）", required = true)
    })
    @MethodLog(remark = "批量标记消息为已读")
    @PutMapping("/batchRead")
    public ApiRes batchMarkAsRead() {
        List<Long> msgIds = getValLongList("msgIds");
        Long currentUserId = getCurrentUser().getSysUser().getSysUserId();
        int count = sysMessageService.batchMarkAsRead(msgIds, currentUserId);
        return ApiRes.ok(count);
    }

    /**
     * 删除消息
     * @param msgId 消息ID
     * @return 操作结果
     * @since 2025
     */
    @ApiOperation("删除消息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "msgId", value = "消息ID", required = true, dataType = "Long")
    })
    @PreAuthorize("hasAuthority('ENT_MESSAGE_DELETE')")
    @MethodLog(remark = "删除消息")
    @DeleteMapping("/{msgId}")
    public ApiRes delete(@PathVariable("msgId") Long msgId) {
        boolean result = sysMessageService.removeById(msgId);
        return result ? ApiRes.ok() : ApiRes.fail(ApiRes.CODE_ERROR, "删除失败");
    }

    /**
     * 获取WebSocket在线人数
     * @return 在线人数
     * @since 2025
     */
    @ApiOperation("获取在线人数")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header")
    })
    @GetMapping("/onlineCount")
    public ApiRes onlineCount() {
        int count = MessageWebSocketServer.getOnlineCount();
        return ApiRes.ok(count);
    }

}
