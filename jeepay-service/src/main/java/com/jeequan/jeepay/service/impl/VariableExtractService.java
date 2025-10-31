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

import com.jeequan.jeepay.core.entity.MsgVariableDefine;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.core.entity.RefundOrder;
import com.jeequan.jeepay.core.entity.TransferOrder;
import com.jeequan.jeepay.core.service.IMsgVariableDefineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 变量提取服务
 * 负责从订单对象中提取变量值
 * </p>
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@Slf4j
@Service
public class VariableExtractService {

    @Autowired
    private IMsgVariableDefineService msgVariableDefineService;

    /**
     * 从支付订单提取变量
     *
     * @param payOrder 支付订单对象
     * @return 变量映射表
     */
    public Map<String, Object> extractFromPayOrder(PayOrder payOrder) {
        Map<String, Object> variables = new HashMap<>();
        
        if (payOrder == null) {
            return variables;
        }

        // 查询支付订单相关的变量定义
        List<MsgVariableDefine> varDefines = msgVariableDefineService.listByDataSource(
                MsgVariableDefine.DATA_SOURCE_PAY_ORDER);

        if (varDefines != null) {
            for (MsgVariableDefine varDefine : varDefines) {
                Object value = extractFieldValue(payOrder, varDefine.getSourceField());
                if (value != null) {
                    variables.put(varDefine.getVariableCode(), value);
                }
            }
        }

        // 添加系统变量
        addSystemVariables(variables);

        return variables;
    }

    /**
     * 从退款订单提取变量
     *
     * @param refundOrder 退款订单对象
     * @return 变量映射表
     */
    public Map<String, Object> extractFromRefundOrder(RefundOrder refundOrder) {
        Map<String, Object> variables = new HashMap<>();
        
        if (refundOrder == null) {
            return variables;
        }

        // 查询退款订单相关的变量定义
        List<MsgVariableDefine> varDefines = msgVariableDefineService.listByDataSource(
                MsgVariableDefine.DATA_SOURCE_REFUND_ORDER);

        if (varDefines != null) {
            for (MsgVariableDefine varDefine : varDefines) {
                Object value = extractFieldValue(refundOrder, varDefine.getSourceField());
                if (value != null) {
                    variables.put(varDefine.getVariableCode(), value);
                }
            }
        }

        // 添加系统变量
        addSystemVariables(variables);

        return variables;
    }

    /**
     * 从转账订单提取变量
     *
     * @param transferOrder 转账订单对象
     * @return 变量映射表
     */
    public Map<String, Object> extractFromTransferOrder(TransferOrder transferOrder) {
        Map<String, Object> variables = new HashMap<>();
        
        if (transferOrder == null) {
            return variables;
        }

        // 查询转账订单相关的变量定义
        List<MsgVariableDefine> varDefines = msgVariableDefineService.listByDataSource(
                MsgVariableDefine.DATA_SOURCE_TRANSFER_ORDER);

        if (varDefines != null) {
            for (MsgVariableDefine varDefine : varDefines) {
                Object value = extractFieldValue(transferOrder, varDefine.getSourceField());
                if (value != null) {
                    variables.put(varDefine.getVariableCode(), value);
                }
            }
        }

        // 添加系统变量
        addSystemVariables(variables);

        return variables;
    }

    /**
     * 通过反射提取对象字段值
     *
     * @param object 目标对象
     * @param fieldName 字段名（驼峰命名）
     * @return 字段值
     */
    private Object extractFieldValue(Object object, String fieldName) {
        if (object == null || fieldName == null) {
            return null;
        }

        try {
            Class<?> clazz = object.getClass();
            Field field = findField(clazz, fieldName);
            
            if (field != null) {
                field.setAccessible(true);
                return field.get(object);
            }
        } catch (Exception e) {
            log.warn("提取字段值失败: fieldName={}, error={}", fieldName, e.getMessage());
        }

        return null;
    }

    /**
     * 查找字段（支持继承）
     */
    private Field findField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return findField(clazz.getSuperclass(), fieldName);
            }
        }
        return null;
    }

    /**
     * 添加系统变量
     */
    private void addSystemVariables(Map<String, Object> variables) {
        // 添加请求时间
        variables.put("req_time", new Date());
        
        // 注意：签名变量需要在后续处理中计算
        // variables.put("sign", ""); // 签名将在通知生成时计算
    }

    /**
     * 合并多个变量映射
     *
     * @param maps 多个变量映射
     * @return 合并后的映射
     */
    public Map<String, Object> mergeVariables(Map<String, Object>... maps) {
        Map<String, Object> result = new HashMap<>();
        
        if (maps != null) {
            for (Map<String, Object> map : maps) {
                if (map != null) {
                    result.putAll(map);
                }
            }
        }
        
        return result;
    }
}
