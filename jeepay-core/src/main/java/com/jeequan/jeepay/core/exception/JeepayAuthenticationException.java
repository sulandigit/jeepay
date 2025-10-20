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
package com.jeequan.jeepay.core.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

/**
 * Spring Security 框架自定义异常类
 * <p>
 * 继承自InternalAuthenticationServiceException，用于处理
 * 认证过程中的业务异常，封装BizException对象
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2021/6/15 11:23
 */
@Getter
@Setter
public class JeepayAuthenticationException extends InternalAuthenticationServiceException {

    /**
     * 业务异常对象
     */
    private BizException bizException;

    /**
     * 构造函数
     *
     * @param msg 异常消息
     * @param cause 原始异常
     * @date 2025
     */
    public JeepayAuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * 构造函数
     *
     * @param msg 异常消息
     * @date 2025
     */
    public JeepayAuthenticationException(String msg) {
        super(msg);
    }

    /**
     * 构建认证异常
     *
     * @param msg 异常消息
     * @return JeepayAuthenticationException 认证异常对象
     * @date 2025
     */
    public static JeepayAuthenticationException build(String msg){
        return build(new BizException(msg));
    }

    /**
     * 构建认证异常
     *
     * @param ex 业务异常对象
     * @return JeepayAuthenticationException 认证异常对象
     * @date 2025
     */
    public static JeepayAuthenticationException build(BizException ex){

        JeepayAuthenticationException result = new JeepayAuthenticationException(ex.getMessage());
        result.setBizException(ex);
        return result;
    }

}
