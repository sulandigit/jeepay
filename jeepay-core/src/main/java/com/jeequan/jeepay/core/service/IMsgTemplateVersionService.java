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
import com.jeequan.jeepay.core.entity.MsgTemplateVersion;

/**
 * <p>
 * 消息模板版本服务接口
 * </p>
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
public interface IMsgTemplateVersionService extends IService<MsgTemplateVersion> {

    /**
     * 获取模板的当前生效版本
     */
    MsgTemplateVersion getActiveVersion(Long templateId);

    /**
     * 获取模板的指定版本
     */
    MsgTemplateVersion getByTemplateIdAndVersionNo(Long templateId, Integer versionNo);

    /**
     * 发布版本
     */
    boolean publishVersion(Long versionId, String publishBy);

    /**
     * 归档版本
     */
    boolean archiveVersion(Long versionId);

    /**
     * 恢复版本
     */
    boolean restoreVersion(Long versionId);

    /**
     * 创建新版本（基于当前版本）
     */
    MsgTemplateVersion createNewVersion(Long templateId, String templateContent, String contentFormat, String variableList, String createdBy);
}
