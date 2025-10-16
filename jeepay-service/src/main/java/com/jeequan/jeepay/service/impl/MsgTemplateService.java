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
import com.jeequan.jeepay.core.entity.MsgTemplate;
import com.jeequan.jeepay.core.service.IMsgTemplateService;
import com.jeequan.jeepay.service.mapper.MsgTemplateMapper;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 消息模板服务实现类
 * </p>
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@Service
public class MsgTemplateService extends ServiceImpl<MsgTemplateMapper, MsgTemplate> implements IMsgTemplateService {

    @Override
    public MsgTemplate getByTemplateCode(String templateCode) {
        return getOne(MsgTemplate.gw().eq(MsgTemplate::getTemplateCode, templateCode));
    }

    @Override
    public MsgTemplate getByTemplateType(Byte templateType) {
        return getOne(MsgTemplate.gw()
                .eq(MsgTemplate::getTemplateType, templateType)
                .eq(MsgTemplate::getState, MsgTemplate.STATE_ENABLED)
                .last("LIMIT 1"));
    }
}
