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
import com.jeequan.jeepay.core.entity.MsgVariableDefine;

import java.util.List;

/**
 * <p>
 * 消息变量定义服务接口
 * </p>
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
public interface IMsgVariableDefineService extends IService<MsgVariableDefine> {

    /**
     * 根据变量编码查询变量定义
     */
    MsgVariableDefine getByVariableCode(String variableCode);

    /**
     * 根据数据来源查询变量列表
     */
    List<MsgVariableDefine> listByDataSource(String dataSource);

    /**
     * 批量查询变量定义
     */
    List<MsgVariableDefine> listByVariableCodes(List<String> variableCodes);
}
