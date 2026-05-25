package com.lufax.dashboard.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JsonUtils}.
 * Covers: JSON parsing/serialization, sorted JSON, safe extraction,
 * markdown code block extraction, path navigation, numeric tolerance.
 */
public class JsonUtilsTest {

    // ========== readTree tests ==========

    @Test
    public void testReadTree_ValidJson() {
        Map<String, Object> result = JsonUtils.readTree("{\"key\": \"value\"}");
        assertNotNull(result);
        assertEquals("value", result.get("key"));
    }

    @Test
    public void testReadTree_NestedJson() {
        String json = "{\"outer\": {\"inner\": \"deep\"}}";
        Map<String, Object> result = JsonUtils.readTree(json);

        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>) result.get("outer");
        assertEquals("deep", inner.get("inner"));
    }

    @Test
    public void testReadTree_InvalidJson_ReturnsEmptyMap() {
        Map<String, Object> result = JsonUtils.readTree("not-json");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testReadTree_NullInput() {
        Map<String, Object> result = JsonUtils.readTree(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testReadTree_ArrayAtRoot() {
        Map<String, Object> result = JsonUtils.readTree("[1, 2, 3]");
        assertNotNull(result);
        // Array root returns empty map (expected behavior: read as Map)
    }

    // ========== toJson / toSortedJson tests ==========

    @Test
    public void testToJson_Map() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "test");
        data.put("value", 42);

        String json = JsonUtils.toJson(data);
        assertNotNull(json);
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"test\""));
    }

    @Test
    public void testToJson_NullInput() {
        assertEquals("{}", JsonUtils.toJson(null));
    }

    @Test
    public void testToSortedJson_KeyOrdering() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("zebra", 1);
        data.put("alpha", 2);
        data.put("middle", 3);

        String sorted = JsonUtils.toSortedJson(data);
        // Sorted output should have alpha before zebra
        int alphaIdx = sorted.indexOf("alpha");
        int zebraIdx = sorted.indexOf("zebra");
        assertTrue(alphaIdx < zebraIdx);
    }

    @Test
    public void testToSortedJson_NullInput() {
        assertEquals("{}", JsonUtils.toSortedJson(null));
    }

    // ========== toMap tests ==========

    @Test
    public void testToMap_ValidJson() {
        Map<String, Object> result = JsonUtils.toMap("{\"x\": 1, \"y\": \"abc\"}");
        assertEquals(2, result.size());
        assertEquals(1, result.get("x"));
    }

    @Test
    public void testToMap_InvalidJson_ReturnsEmptyMap() {
        Map<String, Object> result = JsonUtils.toMap("garbage");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== fromJson tests ==========

    @Test
    public void testFromJson_WithClass() {
        String json = "{\"name\":\"test\",\"value\":10}";
        TestPojo pojo = JsonUtils.fromJson(json, TestPojo.class);
        assertNotNull(pojo);
        assertEquals("test", pojo.getName());
        assertEquals(10, pojo.getValue().intValue());
    }

    @Test
    public void testFromJson_InvalidJson_ReturnsNull() {
        assertNull(JsonUtils.fromJson("invalid", TestPojo.class));
    }

    @Test
    public void testFromJson_WithTypeReference() {
        String json = "[{\"k\":\"v\"},{\"k2\":\"v2\"}]";
        List<Map<String, Object>> result = JsonUtils.fromJson(json, new TypeReference<List<Map<String, Object>>>() {});
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFromJson_NullInput() {
        assertNull(JsonUtils.fromJson((String) null, TestPojo.class));
    }

    // ========== safeJsonGet tests ==========

    @Test
    public void testSafeJsonGet_ExistingKey() {
        String result = JsonUtils.safeJsonGet("{\"target\": \"found\"}", "target");
        assertEquals("found", result);
    }

    @Test
    public void testSafeJsonGet_MissingKey() {
        String result = JsonUtils.safeJsonGet("{\"other\": \"val\"}", "target");
        assertNull(result);
    }

    @Test
    public void testSafeJsonGet_InvalidJson() {
        assertNull(JsonUtils.safeJsonGet("not-json", "key"));
    }

    @Test
    public void testSafeJsonGet_NullValueKey() {
        String result = JsonUtils.safeJsonGet("{\"key\": null}", "key");
        assertNull(result); // value is null -> toString would be "null" but code checks null
    }

    // ========== loadsJsonObject tests ==========

    @Test
    public void testLoadsJsonObject_PlainJson() {
        Map<String, Object> result = JsonUtils.loadsJsonObject("{\"status\": \"ok\"}");
        assertNotNull(result);
        assertEquals("ok", result.get("status"));
    }

    @Test
    public void testLoadsJsonObject_MarkdownCodeBlock() {
        String input = "```json\n{\"extracted\": true}\n```";
        Map<String, Object> result = JsonUtils.loadsJsonObject(input);
        assertNotNull(result);
        assertEquals(true, result.get("extracted"));
    }

    @Test
    public void testLoadsJsonObject_MarkdownCodeBlockWithoutLanguageTag() {
        String input = "```\n{\"no_lang\": true}\n```";
        Map<String, Object> result = JsonUtils.loadsJsonObject(input);
        assertNotNull(result);
        assertEquals(true, result.get("no_lang"));
    }

    @Test
    public void testLoadsJsonObject_TextWithEmbeddedJsonBlock() {
        String input = "Some text before {\"embedded\": 42} and after";
        Map<String, Object> result = JsonUtils.loadsJsonObject(input);
        assertNotNull(result);
        assertEquals(42, result.get("embedded"));
    }

    @Test
    public void testLoadsJsonObject_NullInput() {
        Map<String, Object> result = JsonUtils.loadsJsonObject(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLoadsJsonObject_EmptyString() {
        Map<String, Object> result = JsonUtils.loadsJsonObject("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLoadsJsonObject_WhitespaceOnly() {
        Map<String, Object> result = JsonUtils.loadsJsonObject("   \n\t  ");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLoadsJsonObject_CompletelyInvalid() {
        Map<String, Object> result = JsonUtils.loadsJsonObject("just plain text no json here");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== extractFirstString tests ==========

    @Test
    public void testExtractFirstString_MapPath() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("target", "found_it");
        data.put("level1", inner);

        List<Object> paths = Arrays.asList("level1", "target");
        Object result = JsonUtils.extractFirstString(data, paths);
        assertEquals("found_it", result);
    }

    @Test
    public void testExtractFirstString_ListPath() {
        List<Object> data = Arrays.asList(
            Arrays.asList("first", "second", "third")
        );
        List<Object> paths = Arrays.asList(0, 1);
        Object result = JsonUtils.extractFirstString(data, paths);
        assertEquals("second", result);
    }

    @Test
    public void testExtractFirstString_NullInput() {
        assertNull(JsonUtils.extractFirstString(null, Arrays.asList("path")));
    }

    @Test
    public void testExtractFirstString_NullPaths() {
        assertNull(JsonUtils.extractFirstString(Collections.singletonMap("k", "v"), null));
    }

    @Test
    public void testExtractFirstString_EmptyPaths() {
        assertNull(JsonUtils.extractFirstString(Collections.singletonMap("k", "v"), Collections.emptyList()));
    }

    @Test
    public void testExtractFirstString_OutOfBoundsListIndex() {
        List<Object> data = Arrays.asList("only_one");
        List<Object> paths = Arrays.asList(5); // out of bounds
        assertNull(JsonUtils.extractFirstString(data, paths));
    }

    @Test
    public void testExtractFirstString_WrongPathType() {
        // Using String index on a Map where key doesn't exist
        assertNull(JsonUtils.extractFirstString(
            Collections.singletonMap("k", "v"),
            Arrays.asList("nonexistent")
        ));
    }

    // ========== canonicalPayload tests ==========

    @Test
    public void testCanonicalPayload_NormalMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("z", 1);
        payload.put("a", 2);
        String canonical = JsonUtils.canonicalPayload(payload);
        assertNotNull(canonical);
        // Should be sorted: a before z
        int aIdx = canonical.indexOf("a");
        int zIdx = canonical.indexOf("z");
        assertTrue(aIdx < zIdx);
    }

    @Test
    public void testCanonicalPayload_Null() {
        assertEquals("{}", JsonUtils.canonicalPayload(null));
    }

    // ========== numericWithinTolerance tests ==========

    @Test
    public void testNumericWithinTolerance_ExactMatch() {
        assertTrue(JsonUtils.numericWithinTolerance(100.0, 100.0, 0.001));
    }

    @Test
    public void testNumericWithinTolerance_WithinTolerance() {
        assertTrue(JsonUtils.numericWithinTolerance(100.0, 100.05, 0.1));
    }

    @Test
    public void testNumericWithinTolerance_OutsideTolerance() {
        assertFalse(JsonUtils.numericWithinTolerance(100.0, 105.0, 0.1));
    }

    @Test
    public void testNumericWithinTolerance_IntegerInputs() {
        assertTrue(JsonUtils.numericWithinTolerance(100, 101, 2));
    }

    @Test
    public void testNumericWithinTolerance_StringNumberInputs() {
        assertTrue(JsonUtils.numericWithinTolerance("100.5", "100.6", 0.2));
    }

    @Test
    public void testNumericWithinTolerance_BothNull() {
        // Both null are equal per the implementation
        assertTrue(JsonUtils.numericWithinTolerance(null, null, 0.001));
    }

    @Test
    public void testNumericWithinTolerance_OneNullOneNotNull() {
        assertFalse(JsonUtils.numericWithinTolerance(null, 100.0, 0.001));
    }

    @Test
    public void testNumericWithinTolerance_BigDecimalInputs() {
        assertTrue(JsonUtils.numericWithinTolerance(
            BigDecimal.valueOf(99.99), BigDecimal.valueOf(100.01), 0.05));
    }

    @Test
    public void testNumericWithinTolerance_ZeroTolerance() {
        assertTrue(JsonUtils.numericWithinTolerance(50.0, 50.0, 0.0));
        assertFalse(JsonUtils.numericWithinTolerance(50.0, 50.0001, 0.0));
    }

    // ========== Inner helper class for deserialization tests ==========
    public static class TestPojo {
        private String name;
        private Integer value;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
    }
}
