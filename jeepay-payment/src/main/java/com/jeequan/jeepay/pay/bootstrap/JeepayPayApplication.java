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
package com.jeequan.jeepay.pay.bootstrap;

import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.jeequan.jeepay.pay.config.SystemYmlConfig;
import org.hibernate.validator.HibernateValidator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Arrays;

/**
 * Jeepay Payment Application Main Startup Class
 * Jeepay支付应用主启动类
 * <p>
 * This is the main entry point for the Jeepay payment service.
 * It configures essential components including:
 * - FastJSON message converters with security mode enabled
 * - MyBatis-Plus pagination interceptor
 * - Hibernate validator with fail-fast mode
 * - CORS filter for cross-origin requests
 * 这是Jeepay支付服务的主入口点，配置了关键组件包括：
 * - 启用安全模式的FastJSON消息转换器
 * - MyBatis-Plus分页拦截器
 * - 快速失败模式的Hibernate验证器
 * - 跨域请求的CORS过滤器
 *
 * @author terrfly
 * @date 2019/11/7 15:19
 */
@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks / 启用定时任务
@MapperScan("com.jeequan.jeepay.service.mapper")    // MyBatis mapper interface path / Mybatis mapper接口路径
@ComponentScan(basePackages = "com.jeequan.jeepay.*")   // Since MainApplication is not in the project root directory, need to configure basePackages to scan all Spring components / 由于MainApplication没有在项目根目录，需要配置basePackages属性使得成功扫描所有Spring组件
@Configuration
public class JeepayPayApplication {

    /** System YAML configuration / 系统YAML配置 */
    @Autowired private SystemYmlConfig systemYmlConfig;

    /**
     * Main startup function
     * 主启动函数
     *
     * @param args command line arguments / 命令行参数
     */
    public static void main(String[] args) {

        // Start the application / 启动项目
        SpringApplication.run(JeepayPayApplication.class, args);

    }


    /**
     * FastJSON configuration
     * FastJSON配置信息
     * <p>
     * Configures FastJSON as the HTTP message converter with:
     * - Safe mode enabled to prevent security vulnerabilities
     * - Date format: yyyy-MM-dd HH:mm:ss
     * - Support for application/json media types
     * 配置FastJSON作HTTP消息转换器：
     * - 启用安全模式防止安全漏洞
     * - 日期格式：yyyy-MM-dd HH:mm:ss
     * - 支持application/json媒体类型
     *
     * @return HTTP message converters / HTTP消息转换器
     */
    @Bean
    public HttpMessageConverters fastJsonConfig(){

        // Create FastJSON converter / 新建fast-json转换器
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();

        // Enable FastJSON security mode! / 开启 FastJSON 安全模式！
        ParserConfig.getGlobalInstance().setSafeMode(true);

        // FastJSON configuration / fast-json 配置信息
        FastJsonConfig config = new FastJsonConfig();
        config.setDateFormat("yyyy-MM-dd HH:mm:ss");
        converter.setFastJsonConfig(config);

        // Set response Content-Type / 设置响应的 Content-Type
        converter.setSupportedMediaTypes(Arrays.asList(new MediaType[]{MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8}));
        return new HttpMessageConverters(converter);
    }

    /**
     * MyBatis-Plus pagination interceptor
     * MyBatis-Plus分页拦截器
     * <p>
     * Configures pagination behavior for MyBatis-Plus queries.
     * 为MyBatis-Plus查询配置分页行为。
     *
     * @return pagination interceptor / 分页拦截器
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        // Set whether to call back to the first page when the requested page is greater than the maximum page, true: call back to the first page, false: continue to request. Default is false
        // 设置请求的页面大于最大页后操作， true调回到首页，false 继续请求  默认false
        // paginationInterceptor.setOverflow(false);
        // Set maximum single page limit, default is 500 records, -1 means no limit
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        // paginationInterceptor.setLimit(500);
        return paginationInterceptor;
    }

    /**
     * Hibernate validator with fail-fast mode
     * 快速失败模式的Hibernate验证器
     * <p>
     * Configures validator to return immediately upon the first validation failure.
     * 配置验证器在首个验证失败时立即返回。
     *
     * @return validator instance / 验证器实例
     */
    @Bean
    public Validator validator(){

        ValidatorFactory validatorFactory = Validation.byProvider( HibernateValidator.class )
                .configure()
                .failFast( true )  // Enable fail-fast mode / 启用快速失败模式
                .buildValidatorFactory();
        return validatorFactory.getValidator();
    }

    /**
     * CORS filter for cross-origin requests
     * 允许跨域请求的CORS过滤器
     * <p>
     * Configures CORS settings based on system configuration.
     * When enabled, allows:
     * - Credentials (cookies)
     * - All origin patterns
     * - All headers
     * - All HTTP methods
     * 根据系统配置设置CORS。
     * 启用时允许：
     * - 凭证（cookies）
     * - 所有源模式
     * - 所有请求头
     * - 所有HTTP方法
     *
     * @return CORS filter / CORS过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        if(systemYmlConfig.getAllowCors()){
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowCredentials(true);   // Allow sending cookies / 带上cookie信息
//          config.addAllowedOrigin(CorsConfiguration.ALL);  // Allow cross-origin domain names, * means allow any domain / 允许跨域的域名， *表示允许任何域名使用
            config.addAllowedOriginPattern(CorsConfiguration.ALL);  // Use addAllowedOriginPattern to avoid error: When allowCredentials is true, allowedOrigins cannot contain the special value "*" / 使用addAllowedOriginPattern避免出现错误
            config.addAllowedHeader(CorsConfiguration.ALL);   // Allow any request headers / 允许任何请求头
            config.addAllowedMethod(CorsConfiguration.ALL);   // Allow any HTTP methods (POST, GET, etc.) / 允许任何方法（post、get等）
            source.registerCorsConfiguration("/**", config); // CORS configuration applies to all endpoints / CORS配置对所有接口都有效
        }
        return new CorsFilter(source);
    }

}
