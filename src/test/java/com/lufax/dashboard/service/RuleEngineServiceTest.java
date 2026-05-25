package com.lufax.dashboard.service;

import com.lufax.dashboard.model.dto.*;
import com.lufax.dashboard.model.entity.InsightResult;
import com.lufax.dashboard.TestDataFactory;
import com.lufax.dashboard.util.HashUtils;
import com.lufax.dashboard.util.JsonUtils;
import com.lufax.dashboard.util.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RuleEngineService 单元测试 (Mock)
 * 覆盖: 规则加载（缓存/默认）、规则评估（core_metric/dimension/cross_signal 条件）、
 *       场景哈希计算、版本计算、比较操作符
 */
public class RuleEngineServiceTest {

    @Mock
    private JsonUtils jsonUtils;

    @Mock
    private ResourceUtils resourceUtils;

    @Mock
    private HashUtils hashUtils;

    @InjectMocks
    private RuleEngineService ruleEngineService;

    private CoreMetricDto coreMetric;
    private List<DimensionScoreDto> dimensions;
    private List<CrossSignalDto> signals;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        coreMetric = TestDataFactory.createCoreMetric();
        dimensions = TestDataFactory.createDimensionScores();
        signals = TestDataFactory.createCrossSignals();

        when(hashUtils.computeSha256(anyString())).thenReturn("sha256_hash_value_abc123");
        when(hashUtils.computeSha1(anyString())).thenReturn("sha1_version_hash_xyz");
    }

    // ========== loadRules ==========

    @Test
    public void testLoadRules_FileExists_ParsesAndCaches() {
        RulesDto expected = TestDataFactory.createRules();

        when(resourceUtils.readResource("rules/growth_rules.json"))
                .thenReturn("{\"rules\":[],\"score_threshold\":0.5,\"version\":\"2.0\"}");
        when(jsonUtils.fromJson(anyString(), eq(RulesDto.class))).thenReturn(expected);

        RulesDto result = ruleEngineService.loadRules("growth");

        assertNotNull(result);
        assertEquals("2.0", result.getVersion());

        // 第二次调用应命中缓存，不再读取文件
        RulesDto cached = ruleEngineService.loadRules("growth");
        assertSame("缓存返回同一实例", result, cached);
        verify(resourceUtils, times(1)).readResource(anyString()); // 只读一次
    }

    @Test
    public void testLoadRules_FileNotFound_ReturnsDefault() {
        when(resourceUtils.readResource(anyString())).thenReturn("");

        RulesDto result = ruleEngineService.loadRules("nonexistent_scenario");

        assertNotNull(result);
        assertEquals(0.5, result.getScoreThreshold(), 0.001);
        assertEquals("1.0", result.getVersion());
        assertTrue(result.getRules().isEmpty());
    }

    @Test
    public void testLoadRules_InvalidJson_ReturnsDefault() {
        when(resourceUtils.readResource(anyString())).thenReturn("{invalid json content");
        when(jsonUtils.fromJson(anyString(), eq(RulesDto.class)))
                .thenThrow(new RuntimeException("JSON parse error"));

        RulesDto result = ruleEngineService.loadRules("broken");

        assertNotNull(result);
        assertTrue(result.getRules().isEmpty());
    }

    // ========== evaluate - 核心逻辑 ==========

    @Test
    public void testEvaluate_NoTriggeredRules() {
        RulesDto rules = createRulesWithConditions(
                createRule("rule_low_score", 10.0,
                        createCoreMetricCondition("score", ">", 100.0))); // score=85.5 不满足 >100

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggered = (List<Map<String, Object>>) result.get("triggered_rules");
        assertTrue(triggered.isEmpty());
        assertEquals(0.0, (Double) result.get("total_score"), 0.001);
        assertFalse((Boolean) result.get("is_alert"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEvaluate_TriggeredRule_ScoresAndAlerts() {
        // score > 70 应触发 (当前 score=85.5)
        RulesDto rules = createRulesWithConditions(
                createRule("high_score_alert", 8.0,
                        createCoreMetricCondition("score", ">", 70.0)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);

        List<Map<String, Object>> triggered = (List<Map<String, Object>>) result.get("triggered_rules");
        assertEquals(1, triggered.size());
        assertEquals(8.0, (Double) result.get("total_score"), 0.001);

        // scoreThreshold 默认 0.5, total_score=8.0 >= 0.5 → is_alert=true
        assertTrue((Boolean) result.get("is_alert"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEvaluate_MultipleRules_AllTriggered() {
        RulesDto rules = createRulesWithConditions(
                createRule("rule1", 3.0,
                        createCoreMetricCondition("score", ">=", 50.0)),
                createRule("rule2", 4.0,
                        createDimensionCondition("revenue", "<=", 99.0)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);

        List<Map<String, Object>> triggered = (List<Map<String, Object>>) result.get("triggered_rules");
        assertEquals(2, triggered.size());
        assertEquals(7.0, (Double) result.get("total_score"), 0.001); // 3+4
    }

    @Test
    public void testEvaluate_MultipleRules_PartiallyTriggered() {
        RulesDto rules = createRulesWithConditions(
                createRule("rule_pass", 2.0,
                        createCoreMetricCondition("score", ">", 50.0)),   // 85.5 > 50 ✓
                createRule("rule_fail", 5.0,
                        createCoreMetricCondition("count", ">", 10000L)));  // 5000 > 10000 ✗

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> triggered = (List<Map<String, Object>>) result.get("triggered_rules");
        assertEquals(1, triggered.size());
        assertEquals(2.0, (Double) result.get("total_score"), 0.001);
    }

    // ========== evaluate - 条件类型: core_metric ==========

    @Test
    public void testEvaluate_CoreMetricCondition_GreaterThan() {
        RulesDto rules = createRulesWithConditions(
                createRule("gt_test", 1.0,
                        createCoreMetricCondition("score", ">", 80.0)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertTrue((Boolean) result.get("is_alert"));
    }

    @Test
    public void testEvaluate_CoreMetricCondition_LessThan() {
        RulesDto rules = createRulesWithConditions(
                createRule("lt_test", 1.0,
                        createCoreMetricCondition("offset_factor", "<", 1.5)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        // offset_factor=1.2 < 1.5 → should trigger
        assertTrue((Boolean) result.get("is_alert"));
    }

    @Test
    public void testEvaluate_CoreMetricCondition_Equal() {
        RulesDto rules = createRulesWithConditions(
                createRule("eq_test", 1.0,
                        createCoreMetricCondition("score", "==", 85.5)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertTrue((Boolean) result.get("is_alert")); // score == 85.5 ✓
    }

    @Test
    public void testEvaluate_CoreMetricCondition_NotEqual() {
        RulesDto rules = createRulesWithConditions(
                createRule("neq_test", 1.0,
                        createCoreMetricCondition("score", "!=", 999.0)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertTrue((Boolean) result.get("is_alert")); // 85.5 != 999 ✓
    }

    @Test
    public void testEvaluate_CoreMetricCondition_GreaterEqual() {
        RulesDto rules = createRulesWithConditions(
                createRule("ge_test", 1.0,
                        createCoreMetricCondition("score", ">=", 85.5)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertTrue((Boolean) result.get("is_alert")); // 85.5 >= 85.5 ✓
    }

    @Test
    public void testEvaluate_CoreMetricCondition_LessEqual() {
        RulesDto rules = createRulesWithConditions(
                createRule("le_test", 1.0,
                        createCoreMetricCondition("count", "<=", 5000L)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertTrue((Boolean) result.get("is_alert")); // 5000 <= 5000 ✓
    }

    // ========== evaluate - 条件类型: dimension ==========

    @Test
    public void testEvaluate_DimensionCondition_MatchesDimension() {
        // revenue dimension score = 95
        RulesDto rules = createRulesWithConditions(
                createRule("dim_high", 1.0,
                        createDimensionCondition("revenue", ">", 90.0)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertTrue((Boolean) result.get("is_alert"));
    }

    @Test
    public void testEvaluate_DimensionCondition_DimensionNotFound() {
        RulesDto rules = createRulesWithConditions(
                createRule("dim_missing", 1.0,
                        createDimensionCondition("nonexistent_dim", ">", 0.0)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertFalse("不存在的 dimension 应不触发", (Boolean) result.get("is_alert"));
    }

    // ========== evaluate - 条件类型: cross_signal ==========

    @Test
    public void testEvaluate_CrossSignalCondition_MatchesSignal() {
        // market_trend_up score = 7.8
        RulesDto rules = createRulesWithConditions(
                createRule("sig_strong", 1.0,
                        createCrossSignalCondition("market_trend_up", ">", 7.0)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertTrue((Boolean) result.get("is_alert"));
    }

    @Test
    public void testEvaluate_CrossSignalCondition_SignalNotFound() {
        RulesDto rules = createRulesWithConditions(
                createRule("sig_missing", 1.0,
                        createCrossSignalCondition("unknown_signal", ">", 0.0)));

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertFalse("不存在的 signal 应不触发", (Boolean) result.get("is_alert"));
    }

    // ========== evaluate - 多条件 AND 逻辑 ==========

    @Test
    public void testEvaluate_MultipleConditions_AndLogic_AllPass() {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "and_rule");
        rule.put("score", 2.0);
        List<Map<String, Object>> conditions = new ArrayList<>();
        conditions.add(createCoreMetricCondition("score", ">", 80.0));
        conditions.add(createDimensionCondition("revenue", ">", 90.0));
        rule.put("conditions", conditions);

        RulesDto rules = createRulesFromRaw(rule);

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertTrue("两个条件都满足时应触发", (Boolean) result.get("is_alert"));
    }

    @Test
    public void testEvaluate_MultipleConditions_AndLogic_OneFails() {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "and_fail_rule");
        rule.put("score", 2.0);
        List<Map<String, Object>> conditions = new ArrayList<>();
        conditions.add(createCoreMetricCondition("score", ">", 80.0));      // ✓ 85.5>80
        conditions.add(createDimensionCondition("conversion", ">", 90.0));  // ✗ 72<90
        rule.put("conditions", conditions);

        RulesDto rules = createRulesFromRaw(rule);

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertFalse("任一条件不满足则不应触发", (Boolean) result.get("is_alert"));
    }

    // ========== computeScenarioHash ==========

    @Test
    public void testComputeScenarioHash_ReturnsHashString() {
        String hash = ruleEngineService.computeScenarioHash("growth", coreMetric, dimensions);

        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        verify(hashUtils).computeSha256(anyString());
    }

    @Test
    public void testComputeScenarioHash_SameInputSameHash() {
        String hash1 = ruleEngineService.computeScenarioHash("growth", coreMetric, dimensions);
        String hash2 = ruleEngineService.computeScenarioHash("growth", coreMetric, dimensions);

        assertEquals("相同输入应产生相同 hash", hash1, hash2);
    }

    @Test
    public void testComputeScenarioHash_DifferentScenarioDifferentHash() {
        String hashGrowth = ruleEngineService.computeScenarioHash("growth", coreMetric, dimensions);
        String hashDecline = ruleEngineService.computeScenarioHash("decline", coreMetric, dimensions);

        assertNotEquals("不同 scenario 应产生不同 hash", hashGrowth, hashDecline);
    }

    // ========== getScenarioVersion ==========

    @Test
    public void testGetScenarioVersion_ReturnsVersionHash() {
        InsightResult result = new InsightResult();
        result.setScenario("growth");
        result.setScenarioHash("scenario_hash_abc");

        String version = ruleEngineService.getScenarioVersion(result);

        assertNotNull(version);
        assertFalse(version.isEmpty());
        verify(hashUtils).computeSha1("growth:scenario_hash_abc");
    }

    // ========== 边界情况 ==========

    @Test
    public void testEvaluate_EmptyRules_NoTriggers() {
        RulesDto emptyRules = new RulesDto();
        emptyRules.setRules(new ArrayList<>());
        emptyRules.setScoreThreshold(0.5);

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, emptyRules);

        @SuppressWarnings("unchecked")
        List<Object> triggered = (List<Object>) result.get("triggered_rules");
        assertTrue(triggered.isEmpty());
        assertFalse((Boolean) result.get("is_alert"));
    }

    @Test
    public void testEvaluate_RuleWithoutConditions_IgnoresRule() {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "no_cond_rule");
        rule.put("score", 10.0);
        // 无 conditions 键 → 被忽略

        RulesDto rules = createRulesFromRaw(rule);

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertFalse((Boolean) result.get("is_alert"));
    }

    @Test
    public void testEvaluate_UnknownConditionType_Ignored() {
        Map<String, Object> cond = new LinkedHashMap<>();
        cond.put("type", "unknown_type");
        cond.put("field", "x");
        cond.put("operator", ">");
        cond.put("value", 0);

        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "unknown_type_rule");
        rule.put("score", 1.0);
        rule.put("conditions", Collections.singletonList(cond));

        RulesDto rules = createRulesFromRaw(rule);

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertFalse("未知条件类型应不触发", (Boolean) result.get("is_alert"));
    }

    @Test
    public void testEvaluate_StringValueForNumberField_ParsedAsDouble() {
        // value 为字符串数字
        Map<String, Object> cond = new LinkedHashMap<>();
        cond.put("type", "core_metric");
        cond.put("field", "score");
        cond.put("operator", ">");
        cond.put("value", "80"); // 字符串形式

        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "string_val_rule");
        rule.put("score", 1.0);
        rule.put("conditions", Collections.singletonList(cond));

        RulesDto rules = createRulesFromRaw(rule);

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertTrue("字符串数值应被正确解析", (Boolean) result.get("is_alert"));
    }

    @Test
    public void testEvaluate_NonNumericValue_ReturnsFalse() {
        Map<String, Object> cond = new LinkedHashMap<>();
        cond.put("type", "core_metric");
        cond.put("field", "score");
        cond.put("operator", ">");
        cond.put("value", "not_a_number"); // 无法转为数字

        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", "bad_val_rule");
        rule.put("score", 1.0);
        rule.put("conditions", Collections.singletonList(cond));

        RulesDto rules = createRulesFromRaw(rule);

        Map<String, Object> result = ruleEngineService.evaluate(coreMetric, dimensions, signals, rules);
        assertFalse("非数字值应导致条件判断 false", (Boolean) result.get("is_alert"));
    }

    // ========== 辅助方法：构建测试用规则对象 ==========

    private RulesDto createRulesFromRaw(Map<String, Object>... rawRules) {
        RulesDto dto = new RulesDto();
        List<Object> rulesList = new ArrayList<>(Arrays.asList(rawRules));
        dto.setRules(rulesList);
        dto.setScoreThreshold(0.5);
        return dto;
    }

    private RulesDto createRulesWithConditions(Map<String, Object>... rawRules) {
        return createRulesFromRaw(rawRules);
    }

    private Map<String, Object> createRule(String name, double score, Map<String, Object> condition) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("name", name);
        rule.put("score", score);
        rule.put("conditions", Collections.singletonList(condition));
        return rule;
    }

    private Map<String, Object> createCoreMetricCondition(String field, String operator, Object value) {
        Map<String, Object> cond = new LinkedHashMap<>();
        cond.put("type", "core_metric");
        cond.put("field", field);
        cond.put("operator", operator);
        cond.put("value", value);
        return cond;
    }

    private Map<String, Object> createDimensionCondition(String dimension, String operator, double threshold) {
        Map<String, Object> cond = new LinkedHashMap<>();
        cond.put("type", "dimension");
        cond.put("field", dimension);
        cond.put("operator", operator);
        cond.put("value", threshold);
        return cond;
    }

    private Map<String, Object> createCrossSignalCondition(String signalName, String operator, double threshold) {
        Map<String, Object> cond = new LinkedHashMap<>();
        cond.put("type", "cross_signal");
        cond.put("field", signalName);
        cond.put("operator", operator);
        cond.put("value", threshold);
        return cond;
    }
}
