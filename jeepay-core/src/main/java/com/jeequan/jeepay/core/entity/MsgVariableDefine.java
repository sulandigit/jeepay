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
 * 消息变量定义表
 * </p>
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@ApiModel(value = "消息变量定义", description = "")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_msg_variable_define")
public class MsgVariableDefine extends BaseModel implements Serializable {

    //gw
    public static final LambdaQueryWrapper<MsgVariableDefine> gw(){
        return new LambdaQueryWrapper<>();
    }

    private static final long serialVersionUID=1L;

    /**
     * 变量ID
     */
    @ApiModelProperty(value = "变量ID")
    @TableId(value = "variable_id", type = IdType.AUTO)
    private Long variableId;

    /**
     * 变量编码
     */
    @ApiModelProperty(value = "变量编码")
    private String variableCode;

    /**
     * 变量名称
     */
    @ApiModelProperty(value = "变量名称")
    private String variableName;

    /**
     * 数据类型：STRING、NUMBER、DATE、AMOUNT
     */
    @ApiModelProperty(value = "数据类型：STRING、NUMBER、DATE、AMOUNT")
    private String dataType;

    /**
     * 数据来源：PAY_ORDER、REFUND_ORDER、TRANSFER_ORDER、MCH_APP、SYSTEM、COMPUTED
     */
    @ApiModelProperty(value = "数据来源：PAY_ORDER、REFUND_ORDER、TRANSFER_ORDER、MCH_APP、SYSTEM、COMPUTED")
    private String dataSource;

    /**
     * 来源字段名
     */
    @ApiModelProperty(value = "来源字段名")
    private String sourceField;

    /**
     * 格式化规则，如日期格式、金额转换
     */
    @ApiModelProperty(value = "格式化规则")
    private String formatRule;

    /**
     * 默认值
     */
    @ApiModelProperty(value = "默认值")
    private String defaultValue;

    /**
     * 是否必填：0-否，1-是
     */
    @ApiModelProperty(value = "是否必填：0-否，1-是")
    private Byte isRequired;

    /**
     * 备注说明
     */
    @ApiModelProperty(value = "备注说明")
    private String remark;

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
     * 数据类型枚举
     */
    public static final String DATA_TYPE_STRING = "STRING";
    public static final String DATA_TYPE_NUMBER = "NUMBER";
    public static final String DATA_TYPE_DATE = "DATE";
    public static final String DATA_TYPE_AMOUNT = "AMOUNT";

    /**
     * 数据来源枚举
     */
    public static final String DATA_SOURCE_PAY_ORDER = "PAY_ORDER";
    public static final String DATA_SOURCE_REFUND_ORDER = "REFUND_ORDER";
    public static final String DATA_SOURCE_TRANSFER_ORDER = "TRANSFER_ORDER";
    public static final String DATA_SOURCE_MCH_APP = "MCH_APP";
    public static final String DATA_SOURCE_SYSTEM = "SYSTEM";
    public static final String DATA_SOURCE_COMPUTED = "COMPUTED";
}
