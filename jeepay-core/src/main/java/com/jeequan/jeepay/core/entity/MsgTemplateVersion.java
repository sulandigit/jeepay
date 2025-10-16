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
 * 消息模板版本表
 * </p>
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@ApiModel(value = "消息模板版本", description = "")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_msg_template_version")
public class MsgTemplateVersion extends BaseModel implements Serializable {

    //gw
    public static final LambdaQueryWrapper<MsgTemplateVersion> gw(){
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID=1L;

    /**
     * 版本ID
     */
    @ApiModelProperty(value = "版本ID")
    @TableId(value = "version_id", type = IdType.AUTO)
    private Long versionId;

    /**
     * 模板ID
     */
    @ApiModelProperty(value = "模板ID")
    private Long templateId;

    /**
     * 版本号，从1开始递增
     */
    @ApiModelProperty(value = "版本号，从1开始递增")
    private Integer versionNo;

    /**
     * 模板内容，支持变量占位符
     */
    @ApiModelProperty(value = "模板内容，支持变量占位符")
    private String templateContent;

    /**
     * 内容格式：JSON、QUERY_STRING
     */
    @ApiModelProperty(value = "内容格式：JSON、QUERY_STRING")
    private String contentFormat;

    /**
     * 变量列表定义（JSON数组）
     */
    @ApiModelProperty(value = "变量列表定义")
    private String variableList;

    /**
     * 版本状态：0-草稿，1-已发布，2-已归档
     */
    @ApiModelProperty(value = "版本状态：0-草稿，1-已发布，2-已归档")
    private Byte state;

    /**
     * 发布时间
     */
    @ApiModelProperty(value = "发布时间")
    private Date publishTime;

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
     * 内容格式枚举
     */
    public static final String FORMAT_JSON = "JSON";
    public static final String FORMAT_QUERY_STRING = "QUERY_STRING";

    /**
     * 版本状态枚举
     */
    public static final byte STATE_DRAFT = 0;      // 草稿
    public static final byte STATE_PUBLISHED = 1;  // 已发布
    public static final byte STATE_ARCHIVED = 2;   // 已归档
}
