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
package com.jeequan.jeepay.mgr.ctrl.msg;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.entity.MsgTemplateVersion;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.service.IMsgTemplateVersionService;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.impl.TemplateParseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息模板版本管理Controller
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@Api(tags = "消息模板版本管理")
@RestController
@RequestMapping("/api/msgTemplate")
public class MsgTemplateVersionController extends CommonCtrl {

    @Autowired
    private IMsgTemplateVersionService msgTemplateVersionService;

    @Autowired
    private TemplateParseService templateParseService;

    /**
     * 查询模板的所有版本列表
     */
    @ApiOperation("查询版本列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "templateId", value = "模板ID", required = true, dataType = "long"),
            @ApiImplicitParam(name = "state", value = "版本状态", dataType = "byte")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VERSION_LIST')")
    @RequestMapping(value = "/{templateId}/versions", method = RequestMethod.GET)
    public ApiRes versionList(@PathVariable("templateId") Long templateId) {
        JSONObject paramJSON = getReqParamJSON();
        
        LambdaQueryWrapper<MsgTemplateVersion> wrapper = MsgTemplateVersion.gw();
        wrapper.eq(MsgTemplateVersion::getTemplateId, templateId);
        
        Byte state = paramJSON.getByte("state");
        if (state != null) {
            wrapper.eq(MsgTemplateVersion::getState, state);
        }
        
        wrapper.orderByDesc(MsgTemplateVersion::getVersionNo);
        
        List<MsgTemplateVersion> versions = msgTemplateVersionService.list(wrapper);
        return ApiRes.ok(versions);
    }

    /**
     * 查询版本详情
     */
    @ApiOperation("查询版本详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "versionId", value = "版本ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VERSION_VIEW')")
    @RequestMapping(value = "/version/{versionId}", method = RequestMethod.GET)
    public ApiRes versionDetail(@PathVariable("versionId") Long versionId) {
        MsgTemplateVersion version = msgTemplateVersionService.getById(versionId);
        if (version == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }
        return ApiRes.ok(version);
    }

    /**
     * 创建新版本
     */
    @ApiOperation("创建新版本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "templateId", value = "模板ID", required = true, dataType = "long"),
            @ApiImplicitParam(name = "templateContent", value = "模板内容", required = true, dataType = "string"),
            @ApiImplicitParam(name = "contentFormat", value = "内容格式", required = true, dataType = "string"),
            @ApiImplicitParam(name = "variableList", value = "变量列表JSON", required = true, dataType = "string")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VERSION_ADD')")
    @RequestMapping(value = "/{templateId}/version", method = RequestMethod.POST)
    @MethodLog(remark = "创建模板版本")
    public ApiRes createVersion(@PathVariable("templateId") Long templateId) {
        JSONObject paramJSON = getReqParamJSON();
        
        String templateContent = paramJSON.getString("templateContent");
        String contentFormat = paramJSON.getString("contentFormat");
        String variableList = paramJSON.getString("variableList");
        
        // 验证模板中的变量
        List<String> undefinedVars = templateParseService.validateTemplate(templateContent);
        if (!undefinedVars.isEmpty()) {
            return ApiRes.customFail("模板包含未定义的变量: " + String.join(", ", undefinedVars));
        }
        
        MsgTemplateVersion version = msgTemplateVersionService.createNewVersion(
                templateId, templateContent, contentFormat, variableList, getCurrentUser().getRealname());
        
        if (version == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_CREATE);
        }
        return ApiRes.ok(version);
    }

    /**
     * 更新版本内容（仅草稿状态可编辑）
     */
    @ApiOperation("更新版本内容")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "versionId", value = "版本ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VERSION_EDIT')")
    @RequestMapping(value = "/version/{versionId}", method = RequestMethod.PUT)
    @MethodLog(remark = "更新模板版本")
    public ApiRes updateVersion(@PathVariable("versionId") Long versionId) {
        MsgTemplateVersion version = msgTemplateVersionService.getById(versionId);
        if (version == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }
        
        if (version.getState() != MsgTemplateVersion.STATE_DRAFT) {
            return ApiRes.customFail("仅草稿状态的版本可编辑");
        }
        
        JSONObject paramJSON = getReqParamJSON();
        if (paramJSON.containsKey("templateContent")) {
            version.setTemplateContent(paramJSON.getString("templateContent"));
            
            // 验证模板
            List<String> undefinedVars = templateParseService.validateTemplate(version.getTemplateContent());
            if (!undefinedVars.isEmpty()) {
                return ApiRes.customFail("模板包含未定义的变量: " + String.join(", ", undefinedVars));
            }
        }
        if (paramJSON.containsKey("variableList")) {
            version.setVariableList(paramJSON.getString("variableList"));
        }
        
        boolean result = msgTemplateVersionService.updateById(version);
        if (!result) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_UPDATE);
        }
        return ApiRes.ok();
    }

    /**
     * 发布版本
     */
    @ApiOperation("发布版本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "versionId", value = "版本ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VERSION_PUBLISH')")
    @RequestMapping(value = "/version/{versionId}/publish", method = RequestMethod.POST)
    @MethodLog(remark = "发布模板版本")
    public ApiRes publishVersion(@PathVariable("versionId") Long versionId) {
        try {
            boolean result = msgTemplateVersionService.publishVersion(versionId, getCurrentUser().getRealname());
            if (!result) {
                return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_UPDATE);
            }
            // TODO: 发送MQ消息刷新缓存
            return ApiRes.ok();
        } catch (RuntimeException e) {
            return ApiRes.customFail(e.getMessage());
        }
    }

    /**
     * 归档版本
     */
    @ApiOperation("归档版本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "versionId", value = "版本ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VERSION_ARCHIVE')")
    @RequestMapping(value = "/version/{versionId}/archive", method = RequestMethod.POST)
    @MethodLog(remark = "归档模板版本")
    public ApiRes archiveVersion(@PathVariable("versionId") Long versionId) {
        try {
            boolean result = msgTemplateVersionService.archiveVersion(versionId);
            if (!result) {
                return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_UPDATE);
            }
            return ApiRes.ok();
        } catch (RuntimeException e) {
            return ApiRes.customFail(e.getMessage());
        }
    }

    /**
     * 恢复版本
     */
    @ApiOperation("恢复版本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "versionId", value = "版本ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VERSION_RESTORE')")
    @RequestMapping(value = "/version/{versionId}/restore", method = RequestMethod.POST)
    @MethodLog(remark = "恢复模板版本")
    public ApiRes restoreVersion(@PathVariable("versionId") Long versionId) {
        try {
            boolean result = msgTemplateVersionService.restoreVersion(versionId);
            if (!result) {
                return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_UPDATE);
            }
            // TODO: 发送MQ消息刷新缓存
            return ApiRes.ok();
        } catch (RuntimeException e) {
            return ApiRes.customFail(e.getMessage());
        }
    }

    /**
     * 模板预览
     */
    @ApiOperation("模板预览")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "versionId", value = "版本ID", required = true, dataType = "long"),
            @ApiImplicitParam(name = "testData", value = "测试数据JSON", required = true, dataType = "string")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VERSION_VIEW')")
    @RequestMapping(value = "/version/{versionId}/preview", method = RequestMethod.POST)
    public ApiRes previewVersion(@PathVariable("versionId") Long versionId) {
        MsgTemplateVersion version = msgTemplateVersionService.getById(versionId);
        if (version == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }
        
        JSONObject paramJSON = getReqParamJSON();
        JSONObject testData = paramJSON.getJSONObject("testData");
        
        if (testData == null) {
            testData = new JSONObject();
        }
        
        // 转换为Map
        Map<String, Object> variables = new HashMap<>(testData);
        
        // 解析模板
        String result = templateParseService.parseTemplate(version.getTemplateContent(), variables);
        
        return ApiRes.ok(result);
    }

    /**
     * 删除草稿版本
     */
    @ApiOperation("删除草稿版本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "versionId", value = "版本ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VERSION_DEL')")
    @RequestMapping(value = "/version/{versionId}", method = RequestMethod.DELETE)
    @MethodLog(remark = "删除模板版本")
    public ApiRes deleteVersion(@PathVariable("versionId") Long versionId) {
        MsgTemplateVersion version = msgTemplateVersionService.getById(versionId);
        if (version == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }
        
        if (version.getState() != MsgTemplateVersion.STATE_DRAFT) {
            return ApiRes.customFail("仅可删除草稿状态的版本");
        }
        
        boolean result = msgTemplateVersionService.removeById(versionId);
        if (!result) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_DELETE);
        }
        return ApiRes.ok();
    }
}
