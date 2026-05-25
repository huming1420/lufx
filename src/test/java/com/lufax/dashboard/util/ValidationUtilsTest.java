package com.lufax.dashboard.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * ValidationUtils 单元测试
 * 覆盖: JSON Schema 校验、fallback 校验、null/空值边界
 */
public class ValidationUtilsTest {

    private static final String VALID_JSON = "{\"summary\":\"test summary\",\"analysis\":\"analysis detail\",\"recommendation\":\"buy\"}";
    private static final String VALID_DASHBOARD_JSON = "{\"executive_summary\":\"overview\",\"dimensions\":{}}";
    private static final String VALID_SCENARIO_JSON = "{\"scenario_label\":\"growth\",\"projected_net_profit\":100000}";
    private static final String INVALID_JSON = "{\"summary\":\"only summary\"}";

    // ========== validateAgainstSchema ==========

    @Test
    public void testValidateAgainstSchema_NullJson_ReturnsFalse() {
        boolean result = ValidationUtils.validateAgainstSchema(null, "schema.json");
        assertFalse("null JSON 字符串应返回 false", result);
    }

    @Test
    public void testValidateAgainstSchema_EmptyJson_ReturnsFalse() {
        assertFalse(ValidationUtils.validateAgainstSchema("", "schema.json"));
        assertFalse(ValidationUtils.validateAgainstSchema("   ", "schema.json"));
    }

    @Test
    public void testValidateAgainstSchema_NonExistentSchema_ReturnsFalse() {
        // Schema 文件不存在时，校验应返回 false（不会抛异常）
        boolean result = ValidationUtils.validateAgainstSchema(VALID_JSON, "nonexistent_schema_12345.json");
        assertFalse("不存在的 schema 文件应返回 false", result);
    }

    @Test
    public void testValidateAgainstSchema_InvalidJsonFormat() {
        // 非法 JSON 格式
        boolean result = ValidationUtils.validateAgainstSchema("{invalid json}", "nonexistent.json");
        assertFalse(result); // 解析失败 → false
    }

    // ========== validateWithDetails ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testValidateWithDetails_NullJson_ReturnsErrorMap() {
        Map<String, Object> result = ValidationUtils.validateWithDetails(null, "schema.json");
        assertNotNull(result);
        assertFalse((Boolean) result.get("valid"));
        assertNotNull(result.get("errors"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValidateWithDetails_EmptyJson_ErrorMessage() {
        Map<String, Object> result = ValidationUtils.validateWithDetails("", "schema.json");
        assertFalse((Boolean) result.get("valid"));
        assertNotNull(result.get("errors"));
        // 空输入应有错误信息
        assertTrue(((java.util.List<String>) result.get("errors")).size() > 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testValidateWithDetails_MissingSchema_ReturnsInvalid() {
        Map<String, Object> result = ValidationUtils.validateWithDetails(VALID_JSON, "missing_schema_xyz.json");
        assertFalse("缺少 schema 文件时应 invalid", (Boolean) result.get("valid"));
    }

    // ========== fallbackValidate - card 类型 ==========

    @Test
    public void testFallbackValidate_Card_AllFieldsPresent() {
        assertTrue(ValidationUtils.fallbackValidate(VALID_JSON, "card"));
    }

    @Test
    public void testFallbackValidate_Card_MissingAnalysis() {
        String json = "{\"summary\":\"test\",\"recommendation\":\"buy\"}"; // 缺少 analysis
        assertFalse(ValidationUtils.fallbackValidate(json, "card"));
    }

    @Test
    public void testFallbackValidate_Card_MissingRecommendation() {
        String json = "{\"summary\":\"test\",\"analysis\":\"detail\"}"; // 缺少 recommendation
        assertFalse(ValidationUtils.fallbackValidate(json, "card"));
    }

    @Test
    public void testFallbackValidate_Card_EmptyFieldsStillPasses() {
        String json = "{\"summary\":\"\",\"analysis\":\"\",\"recommendation\":\"\"}"; // 空字符串但有 key
        assertTrue("fallback 只检查 key 存在性", ValidationUtils.fallbackValidate(json, "card"));
    }

    // ========== fallbackValidate - dashboard 类型 ==========

    @Test
    public void testFallbackValidate_Dashboard_AllFieldsPresent() {
        assertTrue(ValidationUtils.fallbackValidate(VALID_DASHBOARD_JSON, "dashboard"));
    }

    @Test
    public void testFallbackValidate_Dashboard_MissingDimensions() {
        String json = "{\"executive_summary\":\"overview\"}"; // 缺少 dimensions
        assertFalse(ValidationUtils.fallbackValidate(json, "dashboard"));
    }

    // ========== fallbackValidate - scenario 类型 ==========

    @Test
    public void testFallbackValidate_Scenario_AllFieldsPresent() {
        assertTrue(ValidationUtils.fallbackValidate(VALID_SCENARIO_JSON, "scenario"));
    }

    @Test
    public void testFallbackValidate_Scenario_MissingProjectedProfit() {
        String json = "{\"scenario_label\":\"growth\"}"; // 缺少 projected_net_profit
        assertFalse(ValidationUtils.fallbackValidate(json, "scenario"));
    }

    // ========== fallbackValidate - 边界情况 ==========

    @Test
    public void testFallbackValidate_NullJson_ReturnsFalse() {
        assertFalse(ValidationUtils.fallbackValidate(null, "card"));
    }

    @Test
    public void testFallbackValidate_EmptyJson_ReturnsFalse() {
        assertFalse(ValidationUtils.fallbackValidate("", "card"));
        assertFalse(ValidationUtils.fallbackValidate("   ", "card"));
    }

    @Test
    public void testFallbackValidate_UnknownType_ReturnsFalse() {
        assertFalse("未知类型应返回 false",
                ValidationUtils.fallbackValidate(VALID_JSON, "unknown_type"));
    }

    @Test
    public void testFallbackValidate_InvalidJson_ReturnsFalse() {
        assertFalse("非法 JSON 格式返回 false",
                ValidationUtils.fallbackValidate("{bad json", "card"));
    }

    // ========== validate (Map + SchemaContent) ==========

    @Test
    public void testValidate_NullData_ReturnsFalse() {
        assertFalse(ValidationUtils.validate(null, "{}"));
    }

    @Test
    public void testValidate_NullSchema_ReturnsFalse() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        assertFalse(ValidationUtils.validate(data, null));
    }

    @Test
    public void testValidate_EmptySchema_ReturnsFalse() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        assertFalse(ValidationUtils.validate(data, ""));
    }

    @Test
    public void testValidate_EmptyData_ReturnsFalse() {
        // 空 map 但非 null，schema 非空 — 实际行为取决于内部实现
        // 当前实现会尝试 toJson 再 validateWithDetails(jsonStr, null)
        // validateWithDetails 中 schemaPath=null 不会读 schema 文件，直接走异常分支
        Map<String, Object> data = new HashMap<>();
        boolean result = ValidationUtils.validate(data, "some schema");
        assertFalse("空 data + 非 null schema 在当前实现中应返回 false", result);
    }

    // ========== fallbackValidation ==========

    @Test
    public void testFallbackValidation_ReturnsSameData() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a", 1);
        input.put("b", "hello");

        Map<String, Object> result = ValidationUtils.fallbackValidation(input);

        assertSame("fallbackValidation 应原样返回传入的 data", input, result);
        assertEquals(2, result.size());
    }

    @Test
    public void testFallbackValidation_NullInput_ReturnsNull() {
        assertNull(ValidationUtils.fallbackValidation(null));
    }

    @Test
    public void testFallbackValidation_EmptyMap_ReturnsEmptyMap() {
        Map<String, Object> empty = new HashMap<>();
        Map<String, Object> result = ValidationUtils.fallbackValidation(empty);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== 复杂 JSON 结构 fallback 测试 ==========

    @Test
    public void testFallbackValidate_Card_NestedStructure() {
        String complexCard = "{" +
                "\"summary\":\"Q3 performance\"," +
                "\"analysis\":{" +
                "   \"trend\":\"up\"," +
                "   \"factors\":[\"seasonal\", \"growth\"]" +
                "}," +
                "\"recommendation\":{\"action\":\"hold\",\"confidence\":0.8}" +
                "}";
        assertTrue("嵌套结构的 card JSON 应通过 fallback",
                ValidationUtils.fallbackValidate(complexCard, "card"));
    }

    @Test
    public void testFallbackValidate_Dashboard_WithNestedDimensions() {
        String complexDashboard = "{" +
                "\"executive_summary\":\"Strong quarter\"," +
                "\"dimensions\":{" +
                "   \"revenue\":{\"value\":100,\"change\":5.2}," +
                "   \"users\":{\"value\":50000,\"change\":-1.3}" +
                "}" +
                "}";
        assertTrue("嵌套 dimensions 的 dashboard JSON 应通过 fallback",
                ValidationUtils.fallbackValidate(complexDashboard, "dashboard"));
    }
}
