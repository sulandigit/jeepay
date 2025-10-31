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
import com.jeequan.jeepay.core.service.IMsgVariableDefineService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * 模板解析服务
 * 负责解析模板内容，提取变量占位符，替换变量值
 * </p>
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@Slf4j
@Service
public class TemplateParseService {

    @Autowired
    private IMsgVariableDefineService msgVariableDefineService;

    /** 变量占位符正则表达式: ${variableName} 或 ${variableName:defaultValue} */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?\\}");

    /**
     * 提取模板中的所有变量占位符
     *
     * @param templateContent 模板内容
     * @return 变量编码列表
     */
    public List<String> extractVariables(String templateContent) {
        if (StringUtils.isBlank(templateContent)) {
            return Collections.emptyList();
        }

        List<String> variables = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!variables.contains(variableName)) {
                variables.add(variableName);
            }
        }

        return variables;
    }

    /**
     * 解析模板，替换变量占位符
     *
     * @param templateContent 模板内容
     * @param variables 变量值映射表
     * @return 替换后的内容
     */
    public String parseTemplate(String templateContent, Map<String, Object> variables) {
        if (StringUtils.isBlank(templateContent)) {
            return "";
        }

        if (variables == null) {
            variables = new HashMap<>();
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String defaultValue = matcher.group(2);

            // 获取变量值
            Object value = variables.get(variableName);
            
            // 如果值为空，使用默认值
            String replacement = "";
            if (value != null) {
                // 获取变量定义，应用格式化规则
                MsgVariableDefine varDefine = msgVariableDefineService.getByVariableCode(variableName);
                if (varDefine != null) {
                    replacement = formatValue(value, varDefine);
                } else {
                    replacement = value.toString();
                }
            } else if (defaultValue != null) {
                replacement = defaultValue;
            }

            // 进行替换（处理特殊字符）
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 根据格式化规则格式化值
     *
     * @param value 原始值
     * @param varDefine 变量定义
     * @return 格式化后的值
     */
    private String formatValue(Object value, MsgVariableDefine varDefine) {
        if (value == null) {
            return StringUtils.defaultString(varDefine.getDefaultValue(), "");
        }

        String formatRule = varDefine.getFormatRule();
        String dataType = varDefine.getDataType();

        try {
            // 金额格式化（分转元）
            if (MsgVariableDefine.DATA_TYPE_AMOUNT.equals(dataType) && "yuan".equals(formatRule)) {
                Long amount = Long.parseLong(value.toString());
                return String.format("%.2f", amount / 100.0);
            }

            // 日期格式化
            if (MsgVariableDefine.DATA_TYPE_DATE.equals(dataType) && StringUtils.isNotBlank(formatRule)) {
                Date date = null;
                if (value instanceof Date) {
                    date = (Date) value;
                } else if (value instanceof Long) {
                    date = new Date((Long) value);
                } else {
                    date = new Date(Long.parseLong(value.toString()));
                }
                SimpleDateFormat sdf = new SimpleDateFormat(formatRule);
                return sdf.format(date);
            }

            // 大写转换
            if ("upper".equals(formatRule)) {
                return value.toString().toUpperCase();
            }

            // 小写转换
            if ("lower".equals(formatRule)) {
                return value.toString().toLowerCase();
            }

            // 状态映射
            if ("stateMap".equals(formatRule)) {
                return mapStateValue(value);
            }

        } catch (Exception e) {
            log.warn("格式化变量值失败: variableCode={}, value={}, formatRule={}, error={}", 
                    varDefine.getVariableCode(), value, formatRule, e.getMessage());
        }

        return value.toString();
    }

    /**
     * 状态值映射
     * 将数字状态码转换为文字描述
     */
    private String mapStateValue(Object value) {
        // 这里可以根据实际业务需求扩展状态映射逻辑
        // 简化处理，直接返回原值
        return value.toString();
    }

    /**
     * 验证模板中的变量是否都已定义
     *
     * @param templateContent 模板内容
     * @return 未定义的变量列表
     */
    public List<String> validateTemplate(String templateContent) {
        List<String> variables = extractVariables(templateContent);
        List<String> undefinedVars = new ArrayList<>();

        for (String variableName : variables) {
            MsgVariableDefine varDefine = msgVariableDefineService.getByVariableCode(variableName);
            if (varDefine == null) {
                undefinedVars.add(variableName);
            }
        }

        return undefinedVars;
    }

    /**
     * 检查必填变量是否都已提供
     *
     * @param variables 变量值映射表
     * @param variableCodes 需要检查的变量编码列表
     * @return 缺失的必填变量列表
     */
    public List<String> checkRequiredVariables(Map<String, Object> variables, List<String> variableCodes) {
        List<String> missingVars = new ArrayList<>();
        
        List<MsgVariableDefine> varDefines = msgVariableDefineService.listByVariableCodes(variableCodes);
        if (varDefines != null) {
            for (MsgVariableDefine varDefine : varDefines) {
                if (varDefine.getIsRequired() == 1) {
                    Object value = variables.get(varDefine.getVariableCode());
                    if (value == null || StringUtils.isBlank(value.toString())) {
                        missingVars.add(varDefine.getVariableCode());
                    }
                }
            }
        }

        return missingVars;
    }
}
