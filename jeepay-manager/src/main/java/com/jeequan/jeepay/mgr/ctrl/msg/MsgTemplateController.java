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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.entity.MsgTemplate;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.service.IMsgTemplateService;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 消息模板管理Controller
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@Api(tags = "消息模板管理")
@RestController
@RequestMapping("/api/msgTemplate")
public class MsgTemplateController extends CommonCtrl {

    @Autowired
    private IMsgTemplateService msgTemplateService;

    /**
     * 模板列表查询
     */
    @ApiOperation("模板列表查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "pageNumber", value = "分页页码", dataType = "int", defaultValue = "1"),
            @ApiImplicitParam(name = "pageSize", value = "分页条数", dataType = "int", defaultValue = "20"),
            @ApiImplicitParam(name = "templateType", value = "模板类型", dataType = "byte"),
            @ApiImplicitParam(name = "state", value = "状态", dataType = "byte"),
            @ApiImplicitParam(name = "templateCode", value = "模板编码", dataType = "string"),
            @ApiImplicitParam(name = "templateName", value = "模板名称", dataType = "string")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_TEMPLATE_LIST')")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ApiPageRes<MsgTemplate> list() {

        MsgTemplate queryObject = getObject(MsgTemplate.class);
        JSONObject paramJSON = getReqParamJSON();

        LambdaQueryWrapper<MsgTemplate> wrapper = MsgTemplate.gw();
        
        if (queryObject.getTemplateType() != null) {
            wrapper.eq(MsgTemplate::getTemplateType, queryObject.getTemplateType());
        }
        if (queryObject.getState() != null) {
            wrapper.eq(MsgTemplate::getState, queryObject.getState());
        }
        if (StringUtils.isNotEmpty(queryObject.getTemplateCode())) {
            wrapper.like(MsgTemplate::getTemplateCode, queryObject.getTemplateCode());
        }
        if (StringUtils.isNotEmpty(queryObject.getTemplateName())) {
            wrapper.like(MsgTemplate::getTemplateName, queryObject.getTemplateName());
        }

        wrapper.orderByDesc(MsgTemplate::getCreatedAt);

        IPage<MsgTemplate> pages = msgTemplateService.page(getIPage(), wrapper);
        return ApiPageRes.pages(pages);
    }

    /**
     * 查询模板详情
     */
    @ApiOperation("查询模板详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "templateId", value = "模板ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_TEMPLATE_VIEW')")
    @RequestMapping(value = "/{templateId}", method = RequestMethod.GET)
    public ApiRes detail(@PathVariable("templateId") Long templateId) {
        MsgTemplate template = msgTemplateService.getById(templateId);
        if (template == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }
        return ApiRes.ok(template);
    }

    /**
     * 创建模板
     */
    @ApiOperation("创建模板")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "templateCode", value = "模板编码", required = true, dataType = "string"),
            @ApiImplicitParam(name = "templateName", value = "模板名称", required = true, dataType = "string"),
            @ApiImplicitParam(name = "templateType", value = "模板类型", required = true, dataType = "byte"),
            @ApiImplicitParam(name = "remark", value = "备注", dataType = "string")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_TEMPLATE_ADD')")
    @RequestMapping(value = "", method = RequestMethod.POST)
    @MethodLog(remark = "创建消息模板")
    public ApiRes add() {
        MsgTemplate template = getObject(MsgTemplate.class);
        template.setCreatedBy(getCurrentUser().getRealname());
        template.setCurrentVersion(0);
        template.setState(MsgTemplate.STATE_ENABLED);
        
        boolean result = msgTemplateService.save(template);
        if (!result) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_CREATE);
        }
        return ApiRes.ok();
    }

    /**
     * 更新模板
     */
    @ApiOperation("更新模板")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "templateId", value = "模板ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_TEMPLATE_EDIT')")
    @RequestMapping(value = "/{templateId}", method = RequestMethod.PUT)
    @MethodLog(remark = "更新消息模板")
    public ApiRes update(@PathVariable("templateId") Long templateId) {
        MsgTemplate template = getObject(MsgTemplate.class);
        template.setTemplateId(templateId);
        
        boolean result = msgTemplateService.updateById(template);
        if (!result) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_UPDATE);
        }
        return ApiRes.ok();
    }

    /**
     * 删除模板
     */
    @ApiOperation("删除模板")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "templateId", value = "模板ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_TEMPLATE_DEL')")
    @RequestMapping(value = "/{templateId}", method = RequestMethod.DELETE)
    @MethodLog(remark = "删除消息模板")
    public ApiRes delete(@PathVariable("templateId") Long templateId) {
        MsgTemplate template = msgTemplateService.getById(templateId);
        if (template == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }
        
        // 检查是否有已发布的版本
        if (template.getCurrentVersion() > 0) {
            return ApiRes.customFail("模板已有发布版本，无法删除");
        }
        
        boolean result = msgTemplateService.removeById(templateId);
        if (!result) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_DELETE);
        }
        return ApiRes.ok();
    }
}
