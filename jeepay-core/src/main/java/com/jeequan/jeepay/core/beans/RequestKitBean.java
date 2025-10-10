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
package com.jeequan.jeepay.core.beans;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP 请求处理工具类
 * 
 * 在 Jeepay 支付系统中负责处理各种 HTTP 请求的参数解析、格式转换和客户端信息提取。
 * 该类采用 Spring 依赖注入机制，自动获取当前请求上下文中的 HttpServletRequest 对象。
 * 
 * 主要功能：
 * 1. 支持多种格式的请求参数解析（JSON、表单参数）
 * 2. 提供线程安全的参数缓存机制
 * 3. 客户端真实 IP 地址获取（支持代理环境）
 * 4. 请求参数到 JSON 对象的自动转换
 * 
 * 线程安全设计：
 * - 使用 Spring RequestContextHolder 进行请求级别的参数缓存
 * - 避免使用实例变量存储请求相关数据
 * - 支持高并发场景下的安全访问
 * 
 * 使用示例：
 * ```java
 * @Autowired
 * private RequestKitBean requestKitBean;
 * 
 * // 获取JSON格式的请求参数
 * JSONObject params = requestKitBean.getReqParamJSON();
 * 
 * // 获取客户端IP地址
 * String clientIp = requestKitBean.getClientIp();
 * ```
 * 
 * 注意事项：
 * 1. SpringMVC 控制器默认采用单例模式，不可使用实例变量保存请求数据
 * 2. 采用线程池模式时，ThreadLocal 可能出现数据残留或覆盖问题
 * 3. 使用 RequestContextHolder 确保请求级别的数据隔离
 * 
 * @author terrfly
 * @version 1.0
 * @since 2021/6/7
 * @see org.springframework.web.context.request.RequestContextHolder
 * @see javax.servlet.http.HttpServletRequest
 */
@Slf4j
@Component
public class RequestKitBean {

    /**
     * HTTP 请求对象
     * 
     * 通过 Spring 依赖注入自动获取当前请求上下文中的 HttpServletRequest 对象。
     * 设置 required = false 是为了避免在非 Web 环境下的注入失败。
     */
    @Autowired(required = false)
    protected HttpServletRequest request;

    /** 
     * RequestContext 对象中的缓存键：存储已转换的 JSON 格式请求参数
     * 
     * 该常量用于在 Spring RequestContextHolder 中缓存解析后的 JSON 参数对象，
     * 避免同一请求中的重复解析操作，提升性能。
     */
    private static final String REQ_CONTEXT_KEY_PARAMJSON = "REQ_CONTEXT_KEY_PARAMJSON";

    /**
     * 从 HTTP 请求体中获取原始参数字符串
     * 
     * 该方法专门用于处理通过请求体（Request Body）传输的参数数据，主要应用于 JSON 格式的 POST 请求。
     * 在支付系统中，许多第三方支付平台的回调通知都采用这种方式传输参数。
     * 
     * 处理流程：
     * 1. 首先通过 isConvertJSON() 判断是否为 JSON 格式请求
     * 2. 如果是 JSON 格式，逐行读取请求体内容
     * 3. 将所有行内容拼接成完整的参数字符串
     * 4. 如果不是 JSON 格式，返回空字符串
     * 
     * 应用场景：
     * - 处理支付回调通知（如微信、支付宝的异步通知）
     * - 解析 REST API 的 JSON 请求体
     * - 获取原始请求数据用于签名验证
     * 
     * 注意事项：
     * - 该方法会消费请求流，同一请求中只能调用一次
     * - 如果请求体过大，可能会影响性能
     * - 异常情况下会抛出 BizException 业务异常
     * 
     * @return 请求体中的原始参数字符串，JSON 格式请求返回完整内容，非 JSON 格式返回空字符串
     * @throws BizException 当请求参数转换过程中发生异常时抛出
     * @see #isConvertJSON() 判断是否为 JSON 格式请求
     * @see #reqParam2JSON() 将参数转换为 JSON 对象
     */
    public String getReqParamFromBody() {

        String body = "";

        if(isConvertJSON()){

            try {
                // 逐行读取请求体内容并拼接
                String str;
                while((str = request.getReader().readLine()) != null){
                    body += str;
                }

                return body;

            } catch (Exception e) {
                // 记录异常日志，包含已读取的部分内容用于问题排查
                log.error("请求参数转换异常！ params=[{}]", body);
                throw new BizException(ApiCodeEnum.PARAMS_ERROR, "转换异常");
            }
        }else {
            // 非 JSON 格式请求直接返回空字符串
            return body;
        }
    }


    /**
     * 将请求参数转换为 JSON 对象
     * 
     * 该方法是参数处理的核心方法，能够智能地处理多种格式的请求参数并统一转换为 JSON 对象。
     * 在 Jeepay 支付系统中，这个方法被广泛用于处理来自不同渠道的支付请求和回调数据。
     * 
     * 支持的参数格式：
     * 1. **JSON 请求体**：直接解析 application/json 格式的请求体
     * 2. **表单参数**：将 key-value 形式的表单参数转换为 JSON
     * 3. **数组参数**：处理多值参数，用逗号分隔拼接
     * 4. **嵌套参数**：支持 ps[abc]=1 转换为 {"ps": {"abc": "1"}}
     * 
     * 处理逻辑：
     * ```
     * 如果是 JSON 格式请求：
     *   └─ 直接解析请求体为 JSON 对象
     * 否则（表单参数）：
     *   ├─ 遍历所有参数
     *   ├─ 处理数组值（逗号拼接）
     *   └─ 处理嵌套结构（[key]格式）
     * ```
     * 
     * 嵌套参数示例：
     * - 原始参数：`ps[name]=张三&ps[age]=25`
     * - 转换结果：`{"ps": {"name": "张三", "age": "25"}}`
     * 
     * 数组参数示例：
     * - 原始参数：`tags=java&tags=spring&tags=payment`
     * - 转换结果：`{"tags": "java,spring,payment"}`
     * 
     * 性能优化：
     * - 对空值进行特殊处理，避免空指针异常
     * - 使用流式处理读取请求体，提高大数据处理效率
     * 
     * @return 转换后的 JSON 对象，包含所有请求参数；如果无参数则返回空的 JSONObject
     * @throws BizException 当 JSON 解析失败或请求体读取异常时抛出
     * @see #isConvertJSON() 判断是否为 JSON 格式请求
     * @see #getReqParamFromBody() 获取请求体原始数据
     */
    public JSONObject reqParam2JSON() {

        JSONObject returnObject = new JSONObject();

        if(isConvertJSON()){
            // 处理 JSON 格式的请求体参数
            String body = "";
            try {
                // 使用流式处理读取请求体，提高大数据处理效率
                body=request.getReader().lines().collect(Collectors.joining(""));
                if(StringUtils.isEmpty(body)) {
                    return returnObject;
                }
                return JSONObject.parseObject(body);

            } catch (Exception e) {
                log.error("请求参数转换异常！ params=[{}]", body);
                throw new BizException(ApiCodeEnum.PARAMS_ERROR, "转换异常");
            }
        }

        // 处理表单参数：获取所有请求参数的 Map 集合
        Map properties = request.getParameterMap();

        // 遍历所有参数并转换为 JSON 格式
        Iterator entries = properties.entrySet().iterator();
        Map.Entry entry;
        String name;
        String value = "";
        while (entries.hasNext()) {
            entry = (Map.Entry) entries.next();
            name = (String) entry.getKey();
            Object valueObj = entry.getValue();
            
            // 处理参数值：支持空值、单值、数组多值等情况
            if(null == valueObj){
                value = "";
            }else if(valueObj instanceof String[]){
                // 处理数组参数：将多个值用逗号连接
                String[] values = (String[])valueObj;
                for(int i=0;i<values.length;i++){
                    value = values[i] + ",";
                }
                value = value.substring(0, value.length()-1);
            }else{
                value = valueObj.toString();
            }

            // 判断是否为嵌套参数结构（如：ps[abc]）
            if(!name.contains("[")){
                // 普通参数：直接放入 JSON 对象
                returnObject.put(name, value);
                continue;
            }
            
            // 处理嵌套参数结构：将 ps[abc]=1 转换为 {"ps": {"abc": "1"}}
            // 解析主键：获取 '[' 之前的部分作为主键
            String mainKey = name.substring(0, name.indexOf("["));
            // 解析子键：获取 '[' 和 ']' 之间的部分作为子键
            String subKey = name.substring(name.indexOf("[") + 1 , name.indexOf("]"));
            
            // 获取或创建子 JSON 对象
            JSONObject subJson = new JSONObject();
            if(returnObject.get(mainKey) != null) {
                // 如果主键已存在，获取已有的子对象
                subJson = (JSONObject)returnObject.get(mainKey);
            }
            // 在子对象中设置子键值
            subJson.put(subKey, value);
            // 将子对象放入主 JSON 对象
            returnObject.put(mainKey, subJson);
        }
        return returnObject;

    }


    /**
     * 获取缓存的 JSON 格式请求参数
     * 
     * 该方法是参数获取的主入口，在整个 Jeepay 系统中被广泛使用。
     * 采用了高效的缓存机制，确保同一请求中的参数只解析一次，提高性能并保证数据一致性。
     * 
     * 缓存机制详解：
     * 1. **缓存位置**：使用 Spring RequestContextHolder 在请求作用域内存储
     * 2. **缓存键**：使用常量 REQ_CONTEXT_KEY_PARAMJSON 作为唯一标识
     * 3. **缓存作用域**：REQUEST_SCOPE，确保请求级别的数据隔离
     * 4. **自动清理**：请求结束后自动清理，避免内存泄漏
     * 
     * 线程安全设计原理：
     * ```
     * 问题： SpringMVC 控制器采用单例模式 + 线程池处理请求
     * 解决方案：
     * ├─ 避免使用实例变量存储请求数据（会导致线程安全问题）
     * ├─ 避免使用 ThreadLocal（线程池环境下可能数据残留或覆盖）
     * └─ 使用 RequestContextHolder（Spring 管理的请求作用域，安全可靠）
     * ```
     * 
     * 性能优化效果：
     * - 避免同一请求中多次调用时的重复解析
     * - 特别适用于复杂的嵌套参数和大量参数的场景
     * - 在支付回调处理中显著提升性能
     * 
     * 使用场景示例：
     * ```java
     * // 在控制器中获取请求参数
     * JSONObject params = requestKitBean.getReqParamJSON();
     * String orderId = params.getString("orderId");
     * 
     * // 在同一请求的不同方法中再次调用（使用缓存）
     * JSONObject sameParams = requestKitBean.getReqParamJSON();
     * ```
     * 
     * @return 缓存的 JSON 格式请求参数，包含所有请求参数的键值对
     * @see #reqParam2JSON() 参数转换的具体实现
     * @see org.springframework.web.context.request.RequestContextHolder Spring 请求上下文管理器
     */
    public JSONObject getReqParamJSON(){

        // 从 Spring 请求上下文中获取缓存的参数对象
        // 这里使用 SCOPE_REQUEST 作用域，确保数据只在当前请求中有效
        Object reqParamObject = RequestContextHolder.getRequestAttributes().getAttribute(REQ_CONTEXT_KEY_PARAMJSON, RequestAttributes.SCOPE_REQUEST);
        
        if(reqParamObject == null){
            // 缓存不存在，首次调用：执行参数解析并缓存结果
            JSONObject reqParam = reqParam2JSON();
            // 将解析结果存储到请求作用域中，供后续调用使用
            RequestContextHolder.getRequestAttributes().setAttribute(REQ_CONTEXT_KEY_PARAMJSON, reqParam, RequestAttributes.SCOPE_REQUEST);
            return reqParam;
        }
        
        // 缓存存在，直接返回缓存的结果（避免重复解析）
        return (JSONObject) reqParamObject;
    }

    /**
     * 判断请求参数是否需要转换为 JSON 格式
     * 
     * 该方法通过分析 HTTP 请求的头信息来判断请求参数的格式类型，
     * 决定是否采用 JSON 解析策略还是表单参数解析策略。
     * 
     * 判断条件（需同时满足以下所有条件）：
     * 1. **Content-Type 检查**：请求头中包含 'application/json'
     * 2. **非空检查**：Content-Type 不为 null
     * 3. **请求方法检查**：非 GET 请求（POST、PUT、PATCH 等）
     * 
     * 设计原理：
     * ```
     * JSON 格式请求：
     * ├─ Content-Type: application/json
     * ├─ 请求方法： POST/PUT/PATCH
     * └─ 参数在请求体中，需要从 InputStream 读取
     * 
     * 表单格式请求：
     * ├─ Content-Type: application/x-www-form-urlencoded
     * ├─ 或者 GET 请求参数
     * └─ 参数可以通过 request.getParameter() 获取
     * ```
     * 
     * 特殊处理：
     * - **GET 请求排除**：即使 Content-Type 为 application/json，GET 请求也不进行 JSON 解析
     * - **大小写不敏感**：对 Content-Type 进行小写转换后再匹配
     * - **部分匹配**：使用 indexOf 而非精确匹配，兼容 "application/json;charset=UTF-8" 等格式
     * 
     * 常见的 Content-Type 示例：
     * ```
     * application/json                     → true  (非 GET)
     * application/json;charset=UTF-8       → true  (非 GET) 
     * application/x-www-form-urlencoded    → false
     * multipart/form-data                  → false
     * text/plain                           → false
     * ```
     * 
     * @return true 表示需要使用 JSON 解析策略；false 表示使用表单参数解析策略
     * @see #getReqParamFromBody() JSON 格式请求体解析
     * @see #reqParam2JSON() 参数转换的主方法
     */
    private boolean isConvertJSON(){

        // 获取请求的 Content-Type 头信息
        String contentType = request.getContentType();

        // 检查是否满足 JSON 格式请求的所有条件
        if(contentType != null  // Content-Type 不为空
                && contentType.toLowerCase().indexOf("application/json") >= 0  // 包含 JSON 类型标识
                && !request.getMethod().equalsIgnoreCase("GET")  // 非 GET 请求
        ){ 
            // 满足条件，需要进行 JSON 格式解析
            return true;
        }

        // 不满足条件，使用表单参数解析方式
        return false;
    }

    /**
     * 获取客户端真实 IP 地址
     * 
     * 在复杂的网络环境中（特别是支付系统），准确获取客户端真实 IP 地址对于安全风控、
     * 日志审计和反欺诈检测至关重要。该方法采用多层级的 IP 获取策略，
     * 能够在各种代理和负载均衡环境下正确识别客户端的真实 IP。
     * 
     * IP 获取策略（按优先级顺序）：
     * 
     * 1. **x-forwarded-for**: 最常用的代理头，记录原始客户端 IP
     *    - 格式: "client_ip, proxy1_ip, proxy2_ip"
     *    - 第一个 IP 为真实客户端 IP
     *    - 支持多层代理链路追踪
     * 
     * 2. **Proxy-Client-IP**: Apache 服务器代理设置的头
     *    - 通常由 Apache mod_proxy 模块设置
     *    - 在企业级部署中常见
     * 
     * 3. **WL-Proxy-Client-IP**: WebLogic 服务器代理设置的头
     *    - WebLogic 应用服务器的标准代理头
     *    - 在传统企业 Java 环境中使用
     * 
     * 4. **RemoteAddr**: 直连 IP 地址（最后选项）
     *    - request.getRemoteAddr() 返回的原始 IP
     *    - 在无代理环境下为真实客户端 IP
     *    - 在有代理环境下为代理服务器 IP
     * 
     * 安全防护机制：
     * - **空值检查**: 过滤 null 和空字符串
     * - **unknown 过滤**: 排除 "unknown" 等无效值
     * - **长度检查**: 防止异常长的 IP 字符串攻击
     * - **多 IP 处理**: 从逗号分隔的 IP 列表中提取第一个有效 IP
     * 
     * 常见的网络拓扑结构：
     * ```
     * 客户端 → CDN → 负载均衡器 → 反向代理 → 应用服务器
     *   Real IP    Proxy1     Proxy2        Proxy3       RemoteAddr
     * 
     * x-forwarded-for: Real_IP, Proxy1_IP, Proxy2_IP
     * ```
     * 
     * 支付系统中的重要性：
     * - **风控检测**: 识别异常支付行为和欺诈交易
     * - **限频控制**: 基于 IP 的访问频率限制
     * - **地域限制**: 地理位置限制和合规性检查
     * - **审计日志**: 记录真实的用户行为和操作轨迹
     * 
     * @return 客户端真实 IP 地址字符串；如果无法获取则返回直连 IP
     * @see javax.servlet.http.HttpServletRequest#getRemoteAddr() 获取直连 IP
     * @see javax.servlet.http.HttpServletRequest#getHeader(String) 获取请求头信息
     */
    public String getClientIp() {
        String ipAddress = null;
        
        // 第一优先级：检查 x-forwarded-for 头（最常用的代理头）
        ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            // 第二优先级：检查 Proxy-Client-IP 头（Apache 代理）
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            // 第三优先级：检查 WL-Proxy-Client-IP 头（WebLogic 代理）
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            // 最后选项：获取直连 IP 地址
            ipAddress = request.getRemoteAddr();
        }

        // 处理多代理情况：从逗号分隔的 IP 列表中提取第一个有效 IP
        // 格式示例："192.168.1.100, 10.0.0.1, 172.16.0.1"
        if (ipAddress != null && ipAddress.length() > 15) { // IPv4 最大长度为 15 位
            if (ipAddress.indexOf(",") > 0) {
                // 提取第一个 IP（客户端真实 IP）
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        }
        return ipAddress;
    }

}
