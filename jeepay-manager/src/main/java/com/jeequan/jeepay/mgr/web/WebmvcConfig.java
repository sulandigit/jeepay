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
package com.jeequan.jeepay.mgr.web;

import com.jeequan.jeepay.core.aop.SignatureInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
* webmvc配置
*
* @author terrfly
* @site https://www.jeequan.com
* @date 2021/6/8 17:12
*/
@Configuration
public class WebmvcConfig implements WebMvcConfigurer {

    @Autowired
    private ApiResInterceptor apiResInterceptor;

    @Autowired
    private SignatureInterceptor signatureInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 签名验证拦截器 - 优先级100
        registry.addInterceptor(signatureInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/anon/**", "/swagger-resources/**")
                .order(100);
        
        // API响应拦截器 - 优先级200
        registry.addInterceptor(apiResInterceptor)
                .order(200);
    }
}
