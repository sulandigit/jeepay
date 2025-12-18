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
package com.jeequan.jeepay.mgr.ctrl.merchant;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.components.mq.model.ResetIsvMchAppInfoConfigMQ;
import com.jeequan.jeepay.components.mq.vender.IMQSender;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.mgr.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.impl.MchAppService;
import com.jeequan.jeepay.service.impl.MchInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 商户应用管理类
 * 负责商户应用的增删改查,包括应用列表查询、新建、详情查询、更新和删除
 *
 * @author zhuxiao
 * @site https://www.jeequan.com
 * @date 2021-06-16 09:15
 */

@Api(tags = "商户应用管理")
@RestController
@RequestMapping("/api/mchApps")
public class MchAppController extends CommonCtrl {

    @Autowired private MchInfoService mchInfoService;
    @Autowired private MchAppService mchAppService;
    @Autowired private IMQSender mqSender;

    /**
     * 应用列表
     * 分页查询商户应用列表
     *
     * @return 应用分页列表
     * @author ZhuXiao
     * @date 2021
    */
    @ApiOperation("查询应用列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "pageNumber", value = "分页页码", dataType = "int", defaultValue = "1"),
            @ApiImplicitParam(name = "pageSize", value = "分页条数", dataType = "int", defaultValue = "20"),
            @ApiImplicitParam(name = "mchNo", value = "商户号"),
            @ApiImplicitParam(name = "appId", value = "应用ID"),
            @ApiImplicitParam(name = "appName", value = "应用名称"),
            @ApiImplicitParam(name = "state", value = "状态: 0-停用, 1-启用", dataType = "Byte")
    })
    @PreAuthorize("hasAuthority('ENT_MCH_APP_LIST')")
    @GetMapping
    public ApiPageRes<MchApp> list() {
        MchApp mchApp = getObject(MchApp.class);

        IPage<MchApp> pages = mchAppService.selectPage(getIPage(), mchApp);
        return ApiPageRes.pages(pages);
    }

    /**
     * 新建应用
     * 为商户创建新的应用并自动生成应用ID
     *
     * @return 操作结果
     * @author ZhuXiao
     * @date 2021
    */
    @ApiOperation("新建应用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "appName", value = "应用名称", required = true),
            @ApiImplicitParam(name = "appSecret", value = "应用私钥", required = true),
            @ApiImplicitParam(name = "mchNo", value = "商户号", required = true),
            @ApiImplicitParam(name = "remark", value = "备注"),
            @ApiImplicitParam(name = "state", value = "状态: 0-停用, 1-启用", dataType = "Byte")
    })
    @PreAuthorize("hasAuthority('ENT_MCH_APP_ADD')")
    @MethodLog(remark = "新建应用")
    @PostMapping
    public ApiRes add() {
        MchApp mchApp = getObject(MchApp.class);
        mchApp.setAppId(IdUtil.objectId());

        if(mchInfoService.getById(mchApp.getMchNo()) == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }

        boolean result = mchAppService.save(mchApp);
        if (!result) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_CREATE);
        }
        return ApiRes.ok();
    }

    /**
     * 应用详情
     * 根据应用ID查询应用详细信息
     *
     * @param appId 应用ID
     * @return 应用详细信息
     * @author ZhuXiao
     * @date 2021
     */
    @ApiOperation("应用详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "appId", value = "应用ID", required = true)
    })
    @PreAuthorize("hasAnyAuthority('ENT_MCH_APP_VIEW', 'ENT_MCH_APP_EDIT')")
    @GetMapping("/{appId}")
    public ApiRes detail(@PathVariable("appId") String appId) {
        MchApp mchApp = mchAppService.selectById(appId);
        if (mchApp == null) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }

        return ApiRes.ok(mchApp);
    }

    /**
     * 更新应用信息
     * 更新应用的基本信息并推送配置变更消息
     *
     * @param appId 应用ID
     * @return 操作结果
     * @author ZhuXiao
     * @date 2021
    */
    @ApiOperation("更新应用信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "appId", value = "应用ID", required = true),
            @ApiImplicitParam(name = "appName", value = "应用名称", required = true),
            @ApiImplicitParam(name = "appSecret", value = "应用私钥", required = true),
            @ApiImplicitParam(name = "mchNo", value = "商户号", required = true),
            @ApiImplicitParam(name = "remark", value = "备注"),
            @ApiImplicitParam(name = "state", value = "状态: 0-停用, 1-启用", dataType = "Byte")
    })
    @PreAuthorize("hasAuthority('ENT_MCH_APP_EDIT')")
    @MethodLog(remark = "更新应用信息")
    @PutMapping("/{appId}")
    public ApiRes update(@PathVariable("appId") String appId) {
        MchApp mchApp = getObject(MchApp.class);
        mchApp.setAppId(appId);
        boolean result = mchAppService.updateById(mchApp);
        if (!result) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_UPDATE);
        }
        // 推送修改应用消息
        mqSender.send(ResetIsvMchAppInfoConfigMQ.build(ResetIsvMchAppInfoConfigMQ.RESET_TYPE_MCH_APP, null, mchApp.getMchNo(), appId));
        return ApiRes.ok();
    }

    /**
     * 删除应用
     * 根据应用ID删除应用并推送配置变更消息
     *
     * @param appId 应用ID
     * @return 操作结果
     * @author ZhuXiao
     * @date 2021
     */
    @ApiOperation("删除应用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "iToken", value = "用户身份凭证", required = true, paramType = "header"),
            @ApiImplicitParam(name = "appId", value = "应用ID", required = true)
    })
    @PreAuthorize("hasAuthority('ENT_MCH_APP_DEL')")
    @MethodLog(remark = "删除应用")
    @DeleteMapping("/{appId}")
    public ApiRes delete(@PathVariable("appId") String appId) {

        MchApp mchApp = mchAppService.getById(appId);
        mchAppService.removeByAppId(appId);

        // 推送mq到目前节点进行更新数据
        mqSender.send(ResetIsvMchAppInfoConfigMQ.build(ResetIsvMchAppInfoConfigMQ.RESET_TYPE_MCH_APP, null, mchApp.getMchNo(), appId));
        return ApiRes.ok();
    }

}