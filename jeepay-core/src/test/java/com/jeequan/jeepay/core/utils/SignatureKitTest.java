package com.jeequan.jeepay.core.utils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * SignatureKit单元测试
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2025-10-17
 */
public class SignatureKitTest {

    private static final String TEST_SECRET = "test_secret_key_123";

    @Test
    public void testMD5Sign() {
        // 准备测试数据
        Map<String, Object> params = new HashMap<>();
        params.put("mchNo", "M1000001");
        params.put("amount", 10000);
        params.put("timestamp", 1622016572190L);
        params.put("nonce", "abc123xyz");
        params.put("signType", "MD5");

        // 执行签名
        String sign = SignatureKit.sign(params, TEST_SECRET, SignatureKit.SIGN_TYPE_MD5);

        // 验证签名不为空
        assertNotNull("签名结果不应为null", sign);
        assertTrue("签名应为32位十六进制字符串", sign.length() == 32);
        assertTrue("签名应为大写", sign.equals(sign.toUpperCase()));

        System.out.println("MD5签名结果: " + sign);
    }

    @Test
    public void testSHA256Sign() {
        // 准备测试数据
        Map<String, Object> params = new HashMap<>();
        params.put("mchNo", "M1000001");
        params.put("amount", 10000);
        params.put("timestamp", 1622016572190L);
        params.put("nonce", "abc123xyz");
        params.put("signType", "SHA256");

        // 执行签名
        String sign = SignatureKit.sign(params, TEST_SECRET, SignatureKit.SIGN_TYPE_SHA256);

        // 验证签名不为空
        assertNotNull("签名结果不应为null", sign);
        assertTrue("签名应为64位十六进制字符串", sign.length() == 64);
        assertTrue("签名应为大写", sign.equals(sign.toUpperCase()));

        System.out.println("SHA256签名结果: " + sign);
    }

    @Test
    public void testVerifySuccess() {
        // 准备测试数据
        Map<String, Object> params = new HashMap<>();
        params.put("mchNo", "M1000001");
        params.put("amount", 10000);
        params.put("timestamp", 1622016572190L);
        params.put("nonce", "abc123xyz");
        params.put("signType", "MD5");

        // 先生成签名
        String sign = SignatureKit.sign(params, TEST_SECRET, SignatureKit.SIGN_TYPE_MD5);
        params.put("sign", sign);

        // 验证签名
        boolean isValid = SignatureKit.verify(params, TEST_SECRET, SignatureKit.SIGN_TYPE_MD5);

        assertTrue("正确的签名应验证通过", isValid);
    }

    @Test
    public void testVerifyFailed() {
        // 准备测试数据
        Map<String, Object> params = new HashMap<>();
        params.put("mchNo", "M1000001");
        params.put("amount", 10000);
        params.put("timestamp", 1622016572190L);
        params.put("nonce", "abc123xyz");
        params.put("signType", "MD5");
        params.put("sign", "WRONG_SIGN_VALUE_123456789ABCDEF");

        // 验证签名
        boolean isValid = SignatureKit.verify(params, TEST_SECRET, SignatureKit.SIGN_TYPE_MD5);

        assertFalse("错误的签名应验证失败", isValid);
    }

    @Test
    public void testSignWithNullValue() {
        // 准备测试数据(包含null值)
        Map<String, Object> params = new HashMap<>();
        params.put("mchNo", "M1000001");
        params.put("amount", 10000);
        params.put("nullParam", null);
        params.put("emptyParam", "");

        // 执行签名
        String sign = SignatureKit.sign(params, TEST_SECRET, SignatureKit.SIGN_TYPE_MD5);

        // 验证签名不为空
        assertNotNull("签名结果不应为null", sign);

        System.out.println("包含空值参数的签名结果: " + sign);
    }

    @Test
    public void testSignConsistency() {
        // 准备测试数据
        Map<String, Object> params = new HashMap<>();
        params.put("mchNo", "M1000001");
        params.put("amount", 10000);

        // 多次签名应得到相同结果
        String sign1 = SignatureKit.sign(params, TEST_SECRET, SignatureKit.SIGN_TYPE_MD5);
        String sign2 = SignatureKit.sign(params, TEST_SECRET, SignatureKit.SIGN_TYPE_MD5);

        assertEquals("相同参数多次签名应得到相同结果", sign1, sign2);
    }

    @Test
    public void testSignWithDifferentOrder() {
        // 准备测试数据1(参数顺序1)
        Map<String, Object> params1 = new HashMap<>();
        params1.put("mchNo", "M1000001");
        params1.put("amount", 10000);
        params1.put("timestamp", 1622016572190L);

        // 准备测试数据2(参数顺序2)
        Map<String, Object> params2 = new HashMap<>();
        params2.put("timestamp", 1622016572190L);
        params2.put("amount", 10000);
        params2.put("mchNo", "M1000001");

        // 签名
        String sign1 = SignatureKit.sign(params1, TEST_SECRET, SignatureKit.SIGN_TYPE_MD5);
        String sign2 = SignatureKit.sign(params2, TEST_SECRET, SignatureKit.SIGN_TYPE_MD5);

        assertEquals("参数顺序不同应得到相同签名结果", sign1, sign2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedAlgorithm() {
        // 准备测试数据
        Map<String, Object> params = new HashMap<>();
        params.put("mchNo", "M1000001");

        // 使用不支持的算法,应抛出异常
        SignatureKit.sign(params, TEST_SECRET, "UNKNOWN_ALGORITHM");
    }
}
