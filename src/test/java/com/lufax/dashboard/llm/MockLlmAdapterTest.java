package com.lufax.dashboard.llm;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * MockLlmAdapter 单元测试
 * 覆盖: 固定返回值、接口契约、参数无关性
 */
public class MockLlmAdapterTest {

    private MockLlmAdapter adapter = new MockLlmAdapter();

    // ========== generate(String) ==========

    @Test
    public void testGenerate_ReturnsNonNull() {
        String result = adapter.generate("any prompt");
        assertNotNull("Mock 响应不应为 null", result);
        assertFalse("Mock 响应不应为空", result.isEmpty());
    }

    @Test
    public void testGenerate_ContainsMockFlag() {
        String result = adapter.generate("test");
        assertTrue("响应应包含 mock 标记", result.contains("\"mock\""));
        assertTrue("响应中 mock 应为 true", result.contains("true"));
    }

    @Test
    public void testGenerate_IsValidJson() {
        String result = adapter.generate("test");
        // 验证返回的是合法 JSON（不抛异常即通过）
        // 简单检查首尾花括号
        assertTrue("响应应为 JSON 格式",
                result.trim().startsWith("{") && result.trim().endsWith("}"));
    }

    @Test
    public void testGenerate_PromptIndependence() {
        // Mock 不关心 prompt 内容，所有调用返回相同格式
        String r1 = adapter.generate(null);
        String r2 = adapter.generate("");
        String r3 = adapter.generate("complex prompt with 中文");

        assertEquals("null prompt 和正常 prompt 返回相同 mock 格式", r1, r2);
        assertEquals("空 prompt 和有内容 prompt 返回相同 mock 格式", r2, r3);
    }

    // ========== generate(String, Map) ==========

    @Test
    public void testGenerateWithParams_ReturnsNonNull() {
        Map<String, Object> params = new HashMap<>();
        params.put("temperature", 0.7);
        params.put("max_tokens", 1000);

        String result = adapter.generate("prompt with params", params);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testGenerateWithParams_ContainsParamsMessage() {
        Map<String, Object> params = new HashMap<>();
        String result = adapter.generate("p", params);
        assertTrue("带参数的响应应包含 'params' 标识",
                result.contains("params"));
    }

    @Test
    public void testGenerateWithParams_NullParams() {
        // null 参数 map 不应抛 NPE
        String result = adapter.generate("test", null);
        assertNotNull("null params 不应导致 null 响应", result);
    }

    @Test
    public void testGenerateWithParams_EmptyParams() {
        Map<String, Object> empty = new HashMap<>();
        String result = adapter.generate("test", empty);
        assertNotNull(result);
    }

    // ========== 接口契约 ==========

    @Test
    public void testImplementsLlmAdapterInterface() {
        assertTrue("MockLlmAdapter 应实现 LlmAdapter 接口",
                adapter instanceof LlmAdapter);
    }

    @Test
    public void testTwoOverloadsReturnDifferentMessages() {
        String noParams = adapter.generate("same prompt");
        Map<String, Object> params = new HashMap<>();
        String withParams = adapter.generate("same prompt", params);

        // 两个重载的 message 不同
        assertNotEquals("两个重载应返回不同的 message 字段", noParams, withParams);
        assertTrue(noParams.contains("Mock LLM response"));
        assertTrue(withParams.contains("Mock LLM response with params"));
    }
}
