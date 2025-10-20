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
package com.jeequan.jeepay.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jeequan.jeepay.core.entity.SysMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 系统站内消息表 Mapper 接口
 * </p>
 *
 * @author [mybatis plus generator]
 * @since 2025
 */
public interface SysMessageMapper extends BaseMapper<SysMessage> {

    /**
     * 查询用户未读消息数量
     * @param userId 用户ID
     * @param sysType 系统类型
     * @return 未读消息数量
     * @since 2025
     */
    int countUnreadByUser(@Param("userId") Long userId, @Param("sysType") String sysType);

    /**
     * 批量标记消息为已读
     * @param msgIds 消息ID列表
     * @param userId 用户ID
     * @return 更新数量
     * @since 2025
     */
    int batchMarkRead(@Param("msgIds") List<Long> msgIds, @Param("userId") Long userId);

}
