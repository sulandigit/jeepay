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

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.beans.RequestKitBean;
import com.jeequan.jeepay.core.exception.SignatureException;
import com.jeequan.jeepay.core.service.SignatureValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 签名验证拦截器
 * 拦截需要验签的请求并执行签名验证
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025-10-17
 */
@Slf4j
@Component
public class SignatureInterceptor implements HandlerInterceptor {

    @Autowired
    private SignatureValidator signatureValidator;

    @Autowired
    private RequestKitBean requestKitBean;

    /**
     * 是否全局启用签名验证
     */
    @Value("${jeepay.signature.enabled:false}")
    private boolean globalEnabled;

    /**
     * 系统级默认密钥
     */
    @Value("${jeepay.signature.default-secret:jeepay_secret_key_2025}")
    private String defaultSecret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        // 只处理方法级别的请求
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 1. 检查方法上是否有@SignRequired注解
        SignRequired methodAnnotation = handlerMethod.getMethodAnnotation(SignRequired.class);
        
        // 2. 检查类上是否有@SignRequired注解
        SignRequired classAnnotation = handlerMethod.getBeanType().getAnnotation(SignRequired.class);
        
        // 3. 确定是否需要验签
        SignRequired signRequired = methodAnnotation != null ? methodAnnotation : classAnnotation;
        
        // 如果没有注解且全局未启用,则不验签
        if (signRequired == null && !globalEnabled) {
            return true;
        }

        // 如果注解明确设置required=false,则不验签
        if (signRequired != null && !signRequired.required()) {
            log.debug("接口[{}]标记为不需要签名验证", request.getRequestURI());
            return true;
        }

        try {
            // 4. 提取请求参数
            Map<String, Object> params = extractParams(request);
            
            log.debug("开始签名验证, URI: {}, 参数: {}", request.getRequestURI(), params);
            
            // 5. 获取签名配置
            boolean checkTimestamp = signRequired != null ? signRequired.checkTimestamp() : true;
            boolean checkNonce = signRequired != null ? signRequired.checkNonce() : false;
            
            // 6. 执行签名验证
            // TODO: 这里可以扩展为从用户信息中获取用户专属密钥
            // 当前使用系统默认密钥
            signatureValidator.validate(params, defaultSecret, checkTimestamp, checkNonce);
            
            log.debug("签名验证通过, URI: {}", request.getRequestURI());
            
            return true;
            
        } catch (SignatureException e) {
            // 签名验证失败,记录日志
            log.warn("签名验证失败, URI: {}, IP: {}, 错误: {}", 
                    request.getRequestURI(), 
                    requestKitBean.getClientIp(), 
                    e.getMessage());
            
            // 抛出异常,由全局异常处理器处理
            throw e;
        }
    }

    /**
     * 提取请求参数
     * 支持GET参数和POST JSON参数
     */
    private Map<String, Object> extractParams(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();
        
        String method = request.getMethod();
        
        if ("GET".equalsIgnoreCase(method)) {
            // GET请求从URL参数中获取
            Map<String, String[]> paramMap = request.getParameterMap();
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                String[] values = entry.getValue();
                if (values != null && values.length > 0) {
                    params.put(entry.getKey(), values[0]);
                }
            }
        } else {
            // POST/PUT/DELETE等请求尝试获取JSON参数
            try {
                JSONObject jsonParams = requestKitBean.getReqParamJSON();
                if (jsonParams != null && !jsonParams.isEmpty()) {
                    params.putAll(jsonParams);
                } else {
                    // 如果不是JSON格式,尝试获取表单参数
                    Map<String, String[]> paramMap = request.getParameterMap();
                    for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                        String[] values = entry.getValue();
                        if (values != null && values.length > 0) {
                            params.put(entry.getKey(), values[0]);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("提取请求参数失败", e);
            }
        }
        
        return params;
    }
}
