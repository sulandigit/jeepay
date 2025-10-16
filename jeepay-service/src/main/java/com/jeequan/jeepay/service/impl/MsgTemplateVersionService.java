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
import com.jeequan.jeepay.core.entity.MsgTemplateVersion;
import com.jeequan.jeepay.core.service.IMsgTemplateService;
import com.jeequan.jeepay.core.service.IMsgTemplateVersionService;
import com.jeequan.jeepay.service.mapper.MsgTemplateVersionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * <p>
 * 消息模板版本服务实现类
 * </p>
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@Service
public class MsgTemplateVersionService extends ServiceImpl<MsgTemplateVersionMapper, MsgTemplateVersion> implements IMsgTemplateVersionService {

    @Autowired
    private IMsgTemplateService msgTemplateService;

    @Override
    public MsgTemplateVersion getActiveVersion(Long templateId) {
        MsgTemplate template = msgTemplateService.getById(templateId);
        if (template == null || template.getCurrentVersion() == 0) {
            return null;
        }
        return getByTemplateIdAndVersionNo(templateId, template.getCurrentVersion());
    }

    @Override
    public MsgTemplateVersion getByTemplateIdAndVersionNo(Long templateId, Integer versionNo) {
        return getOne(MsgTemplateVersion.gw()
                .eq(MsgTemplateVersion::getTemplateId, templateId)
                .eq(MsgTemplateVersion::getVersionNo, versionNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishVersion(Long versionId, String publishBy) {
        MsgTemplateVersion version = getById(versionId);
        if (version == null) {
            throw new RuntimeException("版本不存在");
        }
        if (version.getState() == MsgTemplateVersion.STATE_PUBLISHED) {
            throw new RuntimeException("版本已发布");
        }

        // 归档当前生效的版本
        MsgTemplate template = msgTemplateService.getById(version.getTemplateId());
        if (template != null && template.getCurrentVersion() > 0) {
            MsgTemplateVersion currentVersion = getByTemplateIdAndVersionNo(template.getTemplateId(), template.getCurrentVersion());
            if (currentVersion != null && currentVersion.getState() == MsgTemplateVersion.STATE_PUBLISHED) {
                currentVersion.setState(MsgTemplateVersion.STATE_ARCHIVED);
                updateById(currentVersion);
            }
        }

        // 发布新版本
        version.setState(MsgTemplateVersion.STATE_PUBLISHED);
        version.setPublishTime(new Date());
        boolean updated = updateById(version);

        // 更新模板的当前版本号
        if (updated && template != null) {
            template.setCurrentVersion(version.getVersionNo());
            msgTemplateService.updateById(template);
        }

        return updated;
    }

    @Override
    public boolean archiveVersion(Long versionId) {
        MsgTemplateVersion version = getById(versionId);
        if (version == null) {
            throw new RuntimeException("版本不存在");
        }

        // 检查是否为当前生效版本
        MsgTemplate template = msgTemplateService.getById(version.getTemplateId());
        if (template != null && template.getCurrentVersion().equals(version.getVersionNo())) {
            throw new RuntimeException("当前生效版本不能归档，请先发布其他版本");
        }

        version.setState(MsgTemplateVersion.STATE_ARCHIVED);
        return updateById(version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreVersion(Long versionId) {
        MsgTemplateVersion version = getById(versionId);
        if (version == null) {
            throw new RuntimeException("版本不存在");
        }
        if (version.getState() != MsgTemplateVersion.STATE_ARCHIVED) {
            throw new RuntimeException("只能恢复已归档的版本");
        }

        // 归档当前生效版本
        MsgTemplate template = msgTemplateService.getById(version.getTemplateId());
        if (template != null && template.getCurrentVersion() > 0) {
            MsgTemplateVersion currentVersion = getByTemplateIdAndVersionNo(template.getTemplateId(), template.getCurrentVersion());
            if (currentVersion != null && currentVersion.getState() == MsgTemplateVersion.STATE_PUBLISHED) {
                currentVersion.setState(MsgTemplateVersion.STATE_ARCHIVED);
                updateById(currentVersion);
            }
        }

        // 恢复版本为已发布
        version.setState(MsgTemplateVersion.STATE_PUBLISHED);
        version.setPublishTime(new Date());
        boolean updated = updateById(version);

        // 更新模板的当前版本号
        if (updated && template != null) {
            template.setCurrentVersion(version.getVersionNo());
            msgTemplateService.updateById(template);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MsgTemplateVersion createNewVersion(Long templateId, String templateContent, String contentFormat, String variableList, String createdBy) {
        // 查询当前最大版本号
        MsgTemplateVersion maxVersion = getOne(MsgTemplateVersion.gw()
                .eq(MsgTemplateVersion::getTemplateId, templateId)
                .orderByDesc(MsgTemplateVersion::getVersionNo)
                .last("LIMIT 1"));

        Integer newVersionNo = (maxVersion == null) ? 1 : maxVersion.getVersionNo() + 1;

        // 创建新版本（草稿状态）
        MsgTemplateVersion newVersion = new MsgTemplateVersion();
        newVersion.setTemplateId(templateId);
        newVersion.setVersionNo(newVersionNo);
        newVersion.setTemplateContent(templateContent);
        newVersion.setContentFormat(contentFormat);
        newVersion.setVariableList(variableList);
        newVersion.setState(MsgTemplateVersion.STATE_DRAFT);
        newVersion.setCreatedBy(createdBy);

        save(newVersion);
        return newVersion;
    }
}
