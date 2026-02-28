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

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * BizExceptionResolver 单元测试类
 * 测试全局校验异常处理功能
 *
 * @author jeequan
 * @date 2025/10/15
 */
public class BizExceptionResolverTest {

    private BizExceptionResolver resolver;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        resolver = new BizExceptionResolver();

        // 设置响应 Writer
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

        when(response.getWriter()).thenReturn(printWriter);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    /**
     * 测试用例 TC-001: MethodArgumentNotValidException - 请求体字段为空
     */
    @Test
    public void testMethodArgumentNotValidException_withFieldError() throws Exception {
        // 准备测试数据
        FieldError fieldError = new FieldError("testObject", "mchOrderNo", "商户订单号不能为空");
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        // 执行测试
        resolver.resolveException(request, response, null, exception);

        // 验证结果
        printWriter.flush();
        String result = stringWriter.toString();
        
        assertNotNull("响应内容不应为空", result);
        
        JSONObject jsonObject = JSONObject.parseObject(result);
        assertEquals("错误码应为 PARAMS_ERROR", ApiCodeEnum.PARAMS_ERROR.getCode(), jsonObject.getInteger("code"));
        assertTrue("错误消息应包含校验失败信息", jsonObject.getString("msg").contains("商户订单号不能为空"));
        
        // 验证 response 设置
        verify(response).setContentType(anyString());
        verify(response).getWriter();
    }

    /**
     * 测试用例 TC-002: MethodArgumentNotValidException - 无字段错误（极端情况）
     */
    @Test
    public void testMethodArgumentNotValidException_withoutFieldError() throws Exception {
        // 准备测试数据
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(null);

        // 执行测试
        resolver.resolveException(request, response, null, exception);

        // 验证结果
        printWriter.flush();
        String result = stringWriter.toString();
        
        assertNotNull("响应内容不应为空", result);
        
        JSONObject jsonObject = JSONObject.parseObject(result);
        assertEquals("错误码应为 PARAMS_ERROR", ApiCodeEnum.PARAMS_ERROR.getCode(), jsonObject.getInteger("code"));
        assertTrue("错误消息应包含默认消息", jsonObject.getString("msg").contains("请求参数校验失败"));
    }

    /**
     * 测试用例 TC-003: BindException - 表单参数校验失败
     */
    @Test
    public void testBindException_withFieldError() throws Exception {
        // 准备测试数据
        FieldError fieldError = new FieldError("formObject", "amount", "支付金额不能为空");
        BindException exception = mock(BindException.class);
        
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        // 执行测试
        resolver.resolveException(request, response, null, exception);

        // 验证结果
        printWriter.flush();
        String result = stringWriter.toString();
        
        assertNotNull("响应内容不应为空", result);
        
        JSONObject jsonObject = JSONObject.parseObject(result);
        assertEquals("错误码应为 PARAMS_ERROR", ApiCodeEnum.PARAMS_ERROR.getCode(), jsonObject.getInteger("code"));
        assertTrue("错误消息应包含校验失败信息", jsonObject.getString("msg").contains("支付金额不能为空"));
    }

    /**
     * 测试用例 TC-004: ConstraintViolationException - 单参数校验失败
     */
    @Test
    public void testConstraintViolationException_withViolations() throws Exception {
        // 准备测试数据
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("参数格式不正确");
        
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(violation);
        
        ConstraintViolationException exception = new ConstraintViolationException("校验失败", violations);

        // 执行测试
        resolver.resolveException(request, response, null, exception);

        // 验证结果
        printWriter.flush();
        String result = stringWriter.toString();
        
        assertNotNull("响应内容不应为空", result);
        
        JSONObject jsonObject = JSONObject.parseObject(result);
        assertEquals("错误码应为 PARAMS_ERROR", ApiCodeEnum.PARAMS_ERROR.getCode(), jsonObject.getInteger("code"));
        assertTrue("错误消息应包含校验失败信息", jsonObject.getString("msg").contains("参数格式不正确"));
    }

    /**
     * 测试用例 TC-005: ConstraintViolationException - 无违规信息（极端情况）
     */
    @Test
    public void testConstraintViolationException_withoutViolations() throws Exception {
        // 准备测试数据
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolationException exception = new ConstraintViolationException("校验失败", violations);

        // 执行测试
        resolver.resolveException(request, response, null, exception);

        // 验证结果
        printWriter.flush();
        String result = stringWriter.toString();
        
        assertNotNull("响应内容不应为空", result);
        
        JSONObject jsonObject = JSONObject.parseObject(result);
        assertEquals("错误码应为 PARAMS_ERROR", ApiCodeEnum.PARAMS_ERROR.getCode(), jsonObject.getInteger("code"));
        assertTrue("错误消息应包含默认消息", jsonObject.getString("msg").contains("请求参数校验失败"));
    }

    /**
     * 回归测试: BizException - 确保业务异常处理未受影响
     */
    @Test
    public void testBizException_regression() throws Exception {
        // 准备测试数据
        BizException exception = new BizException("业务处理失败");

        // 执行测试
        resolver.resolveException(request, response, null, exception);

        // 验证结果
        printWriter.flush();
        String result = stringWriter.toString();
        
        assertNotNull("响应内容不应为空", result);
        
        JSONObject jsonObject = JSONObject.parseObject(result);
        assertEquals("错误码应为 CUSTOM_FAIL", ApiCodeEnum.CUSTOM_FAIL.getCode(), jsonObject.getInteger("code"));
    }

    /**
     * 回归测试: DataAccessException - 确保数据库异常处理未受影响
     */
    @Test
    public void testDataAccessException_regression() throws Exception {
        // 准备测试数据
        DataAccessException exception = new DataAccessException("数据库连接失败") {};

        // 执行测试
        resolver.resolveException(request, response, null, exception);

        // 验证结果
        printWriter.flush();
        String result = stringWriter.toString();
        
        assertNotNull("响应内容不应为空", result);
        
        JSONObject jsonObject = JSONObject.parseObject(result);
        assertEquals("错误码应为 DB_ERROR", ApiCodeEnum.DB_ERROR.getCode(), jsonObject.getInteger("code"));
    }

    /**
     * 回归测试: 普通Exception - 确保系统异常处理未受影响
     */
    @Test
    public void testGeneralException_regression() throws Exception {
        // 准备测试数据
        Exception exception = new Exception("系统异常");

        // 执行测试
        resolver.resolveException(request, response, null, exception);

        // 验证结果
        printWriter.flush();
        String result = stringWriter.toString();
        
        assertNotNull("响应内容不应为空", result);
        
        JSONObject jsonObject = JSONObject.parseObject(result);
        assertEquals("错误码应为 SYSTEM_ERROR", ApiCodeEnum.SYSTEM_ERROR.getCode(), jsonObject.getInteger("code"));
    }

    /**
     * 测试用例 TC-006: 多字段校验失败 - 返回第一个错误
     */
    @Test
    public void testMultipleFieldErrors_returnFirstError() throws Exception {
        // 准备测试数据 - 多个字段错误，但只返回第一个
        FieldError firstError = new FieldError("testObject", "mchOrderNo", "商户订单号不能为空");
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(firstError);

        // 执行测试
        resolver.resolveException(request, response, null, exception);

        // 验证结果
        printWriter.flush();
        String result = stringWriter.toString();
        
        assertNotNull("响应内容不应为空", result);
        
        JSONObject jsonObject = JSONObject.parseObject(result);
        assertEquals("错误码应为 PARAMS_ERROR", ApiCodeEnum.PARAMS_ERROR.getCode(), jsonObject.getInteger("code"));
        assertTrue("错误消息应包含第一个字段的错误信息", jsonObject.getString("msg").contains("商户订单号不能为空"));
    }

    /**
     * 测试响应格式 - 验证返回的 JSON 结构符合 ApiRes 规范
     */
    @Test
    public void testResponseFormat_validApiResStructure() throws Exception {
        // 准备测试数据
        FieldError fieldError = new FieldError("testObject", "testField", "测试错误消息");
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        // 执行测试
        resolver.resolveException(request, response, null, exception);

        // 验证结果
        printWriter.flush();
        String result = stringWriter.toString();
        
        JSONObject jsonObject = JSONObject.parseObject(result);
        
        // 验证 ApiRes 结构
        assertTrue("响应应包含 code 字段", jsonObject.containsKey("code"));
        assertTrue("响应应包含 msg 字段", jsonObject.containsKey("msg"));
        assertTrue("响应应包含 data 字段", jsonObject.containsKey("data"));
        assertTrue("响应应包含 sign 字段", jsonObject.containsKey("sign"));
        
        // 验证字段类型和值
        assertNotNull("code 不应为 null", jsonObject.getInteger("code"));
        assertNotNull("msg 不应为 null", jsonObject.getString("msg"));
        assertNull("data 应为 null", jsonObject.get("data"));
        assertNull("sign 应为 null", jsonObject.get("sign"));
    }
}
