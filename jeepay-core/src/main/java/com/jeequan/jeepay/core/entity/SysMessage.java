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
package com.jeequan.jeepay.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jeequan.jeepay.core.model.BaseModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 系统站内消息表
 * </p>
 *
 * @author [mybatis plus generator]
 * @since 2025
 */
@ApiModel(value = "系统站内消息表", description = "")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_sys_message")
public class SysMessage extends BaseModel implements Serializable {

    private static final long serialVersionUID = 1L;

    // 消息类型
    public static final byte TYPE_SYSTEM = 1;  // 系统消息
    public static final byte TYPE_NOTICE = 2;  // 通知消息
    public static final byte TYPE_ALERT = 3;   // 告警消息
    public static final byte TYPE_CUSTOM = 9;  // 自定义消息

    // 消息状态
    public static final byte STATE_UNREAD = 0;  // 未读
    public static final byte STATE_READ = 1;    // 已读

    // 推送方式
    public static final byte PUSH_TYPE_ALL = 1;      // 全体推送
    public static final byte PUSH_TYPE_SPECIFIC = 2; // 指定用户推送

    //gw
    public static final LambdaQueryWrapper<SysMessage> gw(){
        return new LambdaQueryWrapper<>();
    }

    /**
     * 消息ID
     */
    @ApiModelProperty(value = "消息ID")
    @TableId(value = "msg_id", type = IdType.AUTO)
    private Long msgId;

    /**
     * 消息标题
     */
    @ApiModelProperty(value = "消息标题")
    private String title;

    /**
     * 消息内容
     */
    @ApiModelProperty(value = "消息内容")
    private String content;

    /**
     * 消息类型: 1-系统消息, 2-通知消息, 3-告警消息, 9-自定义消息
     */
    @ApiModelProperty(value = "消息类型: 1-系统消息, 2-通知消息, 3-告警消息, 9-自定义消息")
    private Byte msgType;

    /**
     * 接收用户ID
     */
    @ApiModelProperty(value = "接收用户ID")
    private Long receiverUserId;

    /**
     * 接收用户名
     */
    @ApiModelProperty(value = "接收用户名")
    private String receiverUsername;

    /**
     * 发送用户ID
     */
    @ApiModelProperty(value = "发送用户ID")
    private Long senderUserId;

    /**
     * 发送用户名
     */
    @ApiModelProperty(value = "发送用户名")
    private String senderUsername;

    /**
     * 消息状态: 0-未读, 1-已读
     */
    @ApiModelProperty(value = "消息状态: 0-未读, 1-已读")
    private Byte state;

    /**
     * 推送方式: 1-全体推送, 2-指定用户推送
     */
    @ApiModelProperty(value = "推送方式: 1-全体推送, 2-指定用户推送")
    private Byte pushType;

    /**
     * 所属系统: MGR-运营平台, MCH-商户中心
     */
    @ApiModelProperty(value = "所属系统: MGR-运营平台, MCH-商户中心")
    private String sysType;

    /**
     * 读取时间
     */
    @ApiModelProperty(value = "读取时间")
    private Date readTime;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    private Date createdAt;

    /**
     * 更新时间
     */
    @ApiModelProperty(value = "更新时间")
    private Date updatedAt;

}
