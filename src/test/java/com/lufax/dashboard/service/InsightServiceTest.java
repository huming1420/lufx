package com.lufax.dashboard.service;

import com.lufax.dashboard.llm.LlmAdapter;
import com.lufax.dashboard.model.dto.*;
import com.lufax.dashboard.model.entity.InsightResult;
import com.lufax.dashboard.model.request.CardInsightRequest;
import com.lufax.dashboard.model.request.DashboardInsightRequest;
import com.lufax.dashboard.model.request.ScenarioInsightRequest;
import com.lufax.dashboard.repository.InsightResultMapper;
import com.lufax.dashboard.TestDataFactory;
import com.lufax.dashboard.util.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * InsightService 单元测试 (Mock)
 * 覆盖: Dashboard/Card/Scenario 洞察生成、缓存命中、异常降级、保存结果、查询
 */
public class InsightServiceTest {

    @Mock
    private LlmAdapter llmAdapter;

    @Mock
    private FactBuilderService factBuilderService;

    @Mock
    private RuleEngineService ruleEngineService;

    @Mock
    private InsightResultMapper insightResultMapper;

    @Mock
    private JsonUtils jsonUtils;

    @Mock
    private HashUtils hashUtils;

    @Mock
    private ResourceUtils resourceUtils;

    @Mock
    private ValidationUtils validationUtils;

    @InjectMocks
    private InsightService insightService;

    private Map<String, Object> inputData;
    private CoreMetricDto coreMetric;
    private List<DimensionScoreDto> dimensions;
    private List<CrossSignalDto> signals;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        inputData = TestDataFactory.createInputData();
        coreMetric = TestDataFactory.createCoreMetric();
        dimensions = TestDataFactory.createDimensionScores();
        signals = TestDataFactory.createCrossSignals();

        // 默认 mock 行为
        when(factBuilderService.buildCoreMetric(anyMap())).thenReturn(coreMetric);
        when(factBuilderService.buildDimensionScores(anyMap())).thenReturn(dimensions);
        when(factBuilderService.buildCrossSignals(anyMap())).thenReturn(signals);
        when(resourceUtils.readResource(anyString())).thenReturn("");
        when(jsonUtils.toJson(any())).thenReturn("{}");
        when(jsonUtils.fromJson(anyString(), any(Class.class))).thenReturn(new HashMap<>());
        when(validationUtils.validate(any(), anyString())).thenReturn(true);
        when(validationUtils.fallbackValidation(any())).thenReturn(new HashMap<>());
    }

    // ========== generateDashboardInsight - 正常路径 ==========

    @Test
    public void testGenerateDashboardInsight_ReturnsResultWithScenario() {
        DashboardInsightRequest request = TestDataFactory.createDashboardRequest();
        request.setInputData(inputData);

        // Mock 无缓存 + LLM 返回正常 JSON
        Map<String, Object> llmResponse = new LinkedHashMap<>();
        llmResponse.put("executive_summary", "Strong performance");
        llmResponse.put("dimensions", new HashMap<>());

        when(llmAdapter.generate(anyString(), anyMap())).thenReturn("{\"executive_summary\":\"test\"}");

        Map<String, Object> result = insightService.generateDashboardInsight(request);

        assertNotNull(result);
        assertEquals("growth", result.get("scenario"));
        verify(llmAdapter, times(1)).generate(anyString(), anyMap());
    }

    // ========== generateDashboardInsight - 缓存路径 ==========

    @Test
    public void testGenerateDashboardInsight_CacheHit_ReturnsCached() {
        DashboardInsightRequest request = TestDataFactory.createDashboardRequest();
        request.setInputData(inputData);

        // 模拟缓存命中：selectByScenarioAndHash 返回非 null
        InsightResult cachedResult = new InsightResult();
        cachedResult.setId(1L);
        cachedResult.setScenario("growth");
        cachedResult.setInsightJson("{\"cached\":true,\"executive_summary\":\"from cache\"}");

        when(insightResultMapper.selectByScenarioAndHash(anyString(), anyString()))
                .thenReturn(cachedResult);
        when(jsonUtils.fromJson(anyString(), eq(Map.class)))
                .thenAnswer(inv -> {
                    String json = inv.getArgument(0);
                    return jsonUtils.fromJson(json, Map.class); // 使用真实解析
                });
        // 需要返回一个可用的 map
        Map<String, Object> cachedMap = new LinkedHashMap<>();
        cachedMap.put("cached", true);
        cachedMap.put("executive_summary", "from cache");
        when(jsonUtils.fromJson(cachedResult.getInsightJson(), eq(Map.class))).thenReturn(cachedMap);

        Map<String, Object> result = insightService.generateDashboardInsight(request);

        assertTrue("应使用缓存数据", (Boolean) result.get("cached"));
        // 缓存命中时不应调用 LLM
        verify(llmAdapter, never()).generate(anyString(), anyMap());
    }

    // ========== generateDashboardInsight - 异常降级 ==========

    @Test
    public void testGenerateDashboardInsight_Exception_ReturnsFallback() {
        DashboardInsightRequest request = TestDataFactory.createDashboardRequest();
        request.setInputData(inputData);

        // 模拟 LLM 抛异常
        when(llmAdapter.generate(anyString(), anyMap()))
                .thenThrow(new RuntimeException("LLM service unavailable"));

        Map<String, Object> result = insightService.generateDashboardInsight(request);

        assertNotNull(result);
        assertTrue("应包含 error 键", result.containsKey("error"));
        assertTrue("应包含 fallback insights", result.containsKey("insights"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fallbackInsights = (List<Map<String, Object>>) result.get("insights");
        assertFalse("fallback 不应为空", fallbackInsights.isEmpty());
        assertEquals("info", fallbackInsights.get(0).get("type"));
    }

    // ========== generateCardInsight ==========

    @Test
    public void testGenerateCardInsight_ReturnsCardIdInResult() {
        CardInsightRequest request = TestDataFactory.createCardRequest();
        request.setInputData(inputData);

        CardConfigDto config = new CardConfigDto();
        config.setCardId("card_001");
        config.setCardName("Revenue Card");
        when(factBuilderService.loadCardConfig(anyString())).thenReturn(config);

        when(llmAdapter.generate(anyString(), anyMap()))
                .thenReturn("{\"summary\":\"test\",\"analysis\":\"test analysis\",\"recommendation\":\"hold\"}");

        Map<String, Object> result = insightService.generateCardInsight(request);

        assertNotNull(result);
        assertEquals("card_001", result.get("card_id"));
    }

    @Test
    public void testGenerateCardInsight_Exception_ReturnsFallback() {
        CardInsightRequest request = TestDataFactory.createCardRequest();
        request.setInputData(inputData);

        when(factBuilderService.loadCardConfig(anyString()))
                .thenThrow(new RuntimeException("Config load failed"));

        Map<String, Object> result = insightService.generateCardInsight(request);

        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        assertTrue(result.containsKey("insights"));
    }

    // ========== generateScenarioInsight ==========

    @Test
    public void testGenerateScenarioInsight_ReturnsWithRuleEvaluation() {
        ScenarioInsightRequest request = TestDataFactory.createScenarioRequest();
        request.setInputData(inputData);

        RulesDto rules = TestDataFactory.createRules();
        when(ruleEngineService.loadRules(anyString())).thenReturn(rules);

        Map<String, Object> ruleEvalResult = new HashMap<>();
        ruleEvalResult.put("triggered_rules", Collections.emptyList());
        ruleEvalResult.put("total_score", 0.0);
        ruleEvalResult.put("is_alert", false);
        when(ruleEngineService.evaluate(any(), any(), any(), any())).thenReturn(ruleEvalResult);

        when(llmAdapter.generate(anyString(), anyMap()))
                .thenReturn("{\"scenario_label\":\"test\",\"projected_net_profit\":100000}");

        Map<String, Object> result = insightService.generateScenarioInsight(request);

        assertNotNull(result);
        assertTrue(result.containsKey("rule_evaluation"));
        verify(ruleEngineService, atLeastOnce()).evaluate(any(), any(), any(), any());
    }

    @Test
    public void testGenerateScenarioInsight_Exception_ReturnsFallback() {
        ScenarioInsightRequest request = TestDataFactory.createScenarioRequest();
        request.setInputData(inputData);

        when(ruleEngineService.loadRules(anyString()))
                .thenThrow(new RuntimeException("Rules engine failed"));

        Map<String, Object> result = insightService.generateScenarioInsight(request);

        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        assertTrue(result.containsKey("insights"));
    }

    // ========== 查询方法 ==========

    @Test
    public void testGetInsightById_CallsMapper() {
        Long id = 42L;
        InsightResult expected = new InsightResult();
        expected.setId(id);
        expected.setScenario("test");

        when(insightResultMapper.selectById(id)).thenReturn(expected);

        InsightResult result = insightService.getInsightById(id);

        assertNotNull(result);
        assertEquals(id, result.getId());
        verify(insightResultMapper, times(1)).selectById(id);
    }

    @Test
    public void testGetInsightById_NotFound_ReturnsNull() {
        when(insightResultMapper.selectById(999L)).thenReturn(null);
        assertNull(insightService.getInsightById(999L));
    }

    @Test
    public void testGetInsightsByScenario_CallsMapper() {
        List<InsightResult> expected = Arrays.asList(
                TestDataFactory.createInsightResult(1L),
                TestDataFactory.createInsightResult(2L)
        );
        when(insightResultMapper.selectByScenario("growth")).thenReturn(expected);

        List<InsightResult> results = insightService.getInsightsByScenario("growth");

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    public void testGetInsightsByScenarioAndDate_CallsMapper() {
        List<InsightResult> expected = Collections.singletonList(
                TestDataFactory.createInsightResult(3L)
        );
        when(insightResultMapper.selectByScenarioAndDate("growth", "2025-03-21"))
                .thenReturn(expected);

        List<InsightResult> results = insightService.getInsightsByScenarioAndDate("growth", "2025-03-21");

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    // ========== saveInsightResult ==========

    @Test
    public void testSaveInsightResult_CallsUpsert() {
        Map<String, Object> insightData = new LinkedHashMap<>();
        insightData.put("test", "data");

        when(hashUtils.computeSha256(anyString())).thenReturn("abc123hash");
        when(hashUtils.computeSha1(anyString())).thenReturn("version123");

        // 不应抛异常
        insightService.saveInsightResult("test_scenario", "2025-06-15", insightData,
                coreMetric, dimensions);

        verify(insightResultMapper, times(1)).upsert(any(InsightResult.class));
    }

    // ========== 结果结构验证 ==========

    @Test
    public void testGenerateDashboardInsight_ContainsCoreMetricsInResult() {
        DashboardInsightRequest request = TestDataFactory.createDashboardRequest();
        request.setInputData(inputData);

        when(llmAdapter.generate(anyString(), anyMap()))
                .thenReturn("{\"executive_summary\":\"Q2 overview\"}");

        Map<String, Object> result = insightService.generateDashboardInsight(request);

        assertNotNull(result);
        assertTrue("结果应包含 scenario", result.containsKey("scenario"));
        assertTrue("结果应包含 metric_date", result.containsKey("metric_date"));
        assertTrue("结果应包含 report_type", result.containsKey("report_type"));
    }
}
