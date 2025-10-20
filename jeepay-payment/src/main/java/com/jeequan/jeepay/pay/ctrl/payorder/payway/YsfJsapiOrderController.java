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
package com.jeequan.jeepay.pay.ctrl.payorder.payway;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.pay.ctrl.payorder.AbstractPayOrderController;
import com.jeequan.jeepay.pay.rqrs.payorder.payway.YsfJsapiOrderRQ;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UnionPay QuickPass JSAPI Payment Controller
 * 云闪付 jsapi支付 controller
 *
 * This controller handles UnionPay QuickPass (YunShanFu) JSAPI payment requests.
 * JSAPI is typically used for in-app or mini-program payments.
 * It provides API endpoint for merchants to create JSAPI payment orders.
 *
 * @author pangxiaoyu
 * @site https://www.jeequan.com
 * @date 2021/6/8 17:25
 */
@Slf4j
@RestController
// Extends AbstractPayOrderController to inherit common payment order processing logic
public class YsfJsapiOrderController extends AbstractPayOrderController {


    /**
     * Unified Order API Endpoint
     * 统一下单接口
     *
     * This method handles UnionPay QuickPass JSAPI payment order creation requests.
     * It receives merchant payment request, validates the parameters,
     * and processes the order through unified order interface.
     *
     * @return ApiRes API response containing order information or error message
     * @date 2025
     */
    @PostMapping("/api/pay/ysfJsapiOrder")
    public ApiRes aliJsapiOrder(){

        // Get request parameters and validate merchant signature
        // 获取参数 & 验证
        YsfJsapiOrderRQ bizRQ = getRQByWithMchSign(YsfJsapiOrderRQ.class);

        // Process unified order with UnionPay QuickPass JSAPI payment method
        // 统一下单接口
        return unifiedOrder(CS.PAY_WAY_CODE.YSF_JSAPI, bizRQ);

    }


}
