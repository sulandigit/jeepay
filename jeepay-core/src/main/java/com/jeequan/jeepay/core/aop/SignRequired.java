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
package com.jeequan.jeepay.core.aop;

import java.lang.annotation.*;

/**
 * 标记需要签名验证的接口
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025-10-17
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SignRequired {

    /**
     * 是否必须验签
     */
    boolean required() default true;

    /**
     * 签名算法类型: MD5, SHA256
     */
    String algorithm() default "MD5";

    /**
     * 是否校验时间戳
     */
    boolean checkTimestamp() default true;

    /**
     * 是否启用nonce去重(防重放攻击严格模式)
     */
    boolean checkNonce() default false;
}
