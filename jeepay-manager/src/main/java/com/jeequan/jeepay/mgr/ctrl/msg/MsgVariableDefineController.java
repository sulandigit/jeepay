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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.entity.MsgVariableDefine;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.service.IMsgVariableDefineService;
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
 * 消息变量定义管理Controller
 *
 * @author [auto generated]
 * @since 2025-10-16
 */
@Api(tags = "消息变量定义管理")
@RestController
@RequestMapping("/api/msgVariable")
public class MsgVariableDefineController extends CommonCtrl {

    @Autowired
    private IMsgVariableDefineService msgVariableDefineService;

    /**
     * 变量列表查询
     */
    @ApiOperation("变量列表查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "pageNumber", value = "分页页码", dataType = "int", defaultValue = "1"),
            @ApiImplicitParam(name = "pageSize", value = "分页条数", dataType = "int", defaultValue = "20"),
            @ApiImplicitParam(name = "dataSource", value = "数据来源", dataType = "string"),
            @ApiImplicitParam(name = "dataType", value = "数据类型", dataType = "string"),
            @ApiImplicitParam(name = "variableCode", value = "变量编码", dataType = "string"),
            @ApiImplicitParam(name = "variableName", value = "变量名称", dataType = "string")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VARIABLE_LIST')")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public ApiPageRes<MsgVariableDefine> list() {

        MsgVariableDefine queryObject = getObject(MsgVariableDefine.class);

        LambdaQueryWrapper<MsgVariableDefine> wrapper = MsgVariableDefine.gw();

        if (StringUtils.isNotEmpty(queryObject.getDataSource())) {
            wrapper.eq(MsgVariableDefine::getDataSource, queryObject.getDataSource());
        }
        if (StringUtils.isNotEmpty(queryObject.getDataType())) {
            wrapper.eq(MsgVariableDefine::getDataType, queryObject.getDataType());
        }
        if (StringUtils.isNotEmpty(queryObject.getVariableCode())) {
            wrapper.like(MsgVariableDefine::getVariableCode, queryObject.getVariableCode());
        }
        if (StringUtils.isNotEmpty(queryObject.getVariableName())) {
            wrapper.like(MsgVariableDefine::getVariableName, queryObject.getVariableName());
        }

        wrapper.orderByAsc(MsgVariableDefine::getVariableCode);

        IPage<MsgVariableDefine> pages = msgVariableDefineService.page(getIPage(), wrapper);
        return ApiPageRes.pages(pages);
    }

    /**
     * 查询变量详情
     */
    @ApiOperation("查询变量详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "variableId", value = "变量ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VARIABLE_VIEW')")
    @RequestMapping(value = "/{variableId}", method = RequestMethod.GET)
    public ApiRes detail(@PathVariable("variableId") Long variableId) {
        MsgVariableDefine variable = msgVariableDefineService.getById(variableId);
        if (variable == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }
        return ApiRes.ok(variable);
    }

    /**
     * 创建变量定义
     */
    @ApiOperation("创建变量定义")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VARIABLE_ADD')")
    @RequestMapping(value = "", method = RequestMethod.POST)
    @MethodLog(remark = "创建消息变量定义")
    public ApiRes add() {
        MsgVariableDefine variable = getObject(MsgVariableDefine.class);

        // 检查变量编码是否已存在
        MsgVariableDefine existVar = msgVariableDefineService.getByVariableCode(variable.getVariableCode());
        if (existVar != null) {
            return ApiRes.customFail("变量编码已存在");
        }

        boolean result = msgVariableDefineService.save(variable);
        if (!result) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_CREATE);
        }
        return ApiRes.ok();
    }

    /**
     * 更新变量定义
     */
    @ApiOperation("更新变量定义")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "variableId", value = "变量ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VARIABLE_EDIT')")
    @RequestMapping(value = "/{variableId}", method = RequestMethod.PUT)
    @MethodLog(remark = "更新消息变量定义")
    public ApiRes update(@PathVariable("variableId") Long variableId) {
        MsgVariableDefine variable = getObject(MsgVariableDefine.class);
        variable.setVariableId(variableId);

        boolean result = msgVariableDefineService.updateById(variable);
        if (!result) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_UPDATE);
        }
        return ApiRes.ok();
    }

    /**
     * 删除变量定义
     */
    @ApiOperation("删除变量定义")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "variableId", value = "变量ID", required = true, dataType = "long")
    })
    @PreAuthorize("hasAuthority('ENT_MSG_VARIABLE_DEL')")
    @RequestMapping(value = "/{variableId}", method = RequestMethod.DELETE)
    @MethodLog(remark = "删除消息变量定义")
    public ApiRes delete(@PathVariable("variableId") Long variableId) {
        MsgVariableDefine variable = msgVariableDefineService.getById(variableId);
        if (variable == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }

        boolean result = msgVariableDefineService.removeById(variableId);
        if (!result) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_DELETE);
        }
        return ApiRes.ok();
    }
}
