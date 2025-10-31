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
 * 消息模板表
 * </p>
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@ApiModel(value = "消息模板", description = "")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_msg_template")
public class MsgTemplate extends BaseModel implements Serializable {

    //gw
    public static final LambdaQueryWrapper<MsgTemplate> gw(){
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID=1L;

    /**
     * 模板ID
     */
    @ApiModelProperty(value = "模板ID")
    @TableId(value = "template_id", type = IdType.AUTO)
    private Long templateId;

    /**
     * 模板编码
     */
    @ApiModelProperty(value = "模板编码")
    private String templateCode;

    /**
     * 模板名称
     */
    @ApiModelProperty(value = "模板名称")
    private String templateName;

    /**
     * 模板类型：1-支付通知，2-退款通知，3-转账通知
     */
    @ApiModelProperty(value = "模板类型：1-支付通知，2-退款通知，3-转账通知")
    private Byte templateType;

    /**
     * 当前生效版本号
     */
    @ApiModelProperty(value = "当前生效版本号")
    private Integer currentVersion;

    /**
     * 状态：0-停用，1-启用
     */
    @ApiModelProperty(value = "状态：0-停用，1-启用")
    private Byte state;

    /**
     * 备注说明
     */
    @ApiModelProperty(value = "备注说明")
    private String remark;

    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private String createdBy;

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

    /**
     * 模板类型枚举
     */
    public static final byte TYPE_PAY_NOTIFY = 1;      // 支付通知
    public static final byte TYPE_REFUND_NOTIFY = 2;   // 退款通知
    public static final byte TYPE_TRANSFER_NOTIFY = 3; // 转账通知

    /**
     * 状态枚举
     */
    public static final byte STATE_DISABLED = 0;  // 停用
    public static final byte STATE_ENABLED = 1;   // 启用
}
