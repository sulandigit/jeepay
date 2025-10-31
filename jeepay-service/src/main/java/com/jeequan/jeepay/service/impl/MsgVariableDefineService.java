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
import com.jeequan.jeepay.core.entity.MsgVariableDefine;
import com.jeequan.jeepay.core.service.IMsgVariableDefineService;
import com.jeequan.jeepay.service.mapper.MsgVariableDefineMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 消息变量定义服务实现类
 * </p>
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@Service
public class MsgVariableDefineService extends ServiceImpl<MsgVariableDefineMapper, MsgVariableDefine> implements IMsgVariableDefineService {

    @Override
    public MsgVariableDefine getByVariableCode(String variableCode) {
        return getOne(MsgVariableDefine.gw().eq(MsgVariableDefine::getVariableCode, variableCode));
    }

    @Override
    public List<MsgVariableDefine> listByDataSource(String dataSource) {
        return list(MsgVariableDefine.gw().eq(MsgVariableDefine::getDataSource, dataSource));
    }

    @Override
    public List<MsgVariableDefine> listByVariableCodes(List<String> variableCodes) {
        if (variableCodes == null || variableCodes.isEmpty()) {
            return null;
        }
        return list(MsgVariableDefine.gw().in(MsgVariableDefine::getVariableCode, variableCodes));
    }
}
