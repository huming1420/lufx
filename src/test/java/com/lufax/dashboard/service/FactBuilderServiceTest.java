package com.lufax.dashboard.service;

import com.lufax.dashboard.model.dto.*;
import com.lufax.dashboard.TestDataFactory;
import com.lufax.dashboard.util.JsonUtils;
import com.lufax.dashboard.util.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * FactBuilderService 单元测试 (Mock)
 * 覆盖: CoreMetric/Dimension/CrossSignal/RootCause/OffsetFactor/Segment 构建、FactPayload 组装、
 *       ScenarioDefault/CardConfig 加载、数值精度舍入
 */
public class FactBuilderServiceTest {

    @Mock
    private JsonUtils jsonUtils;

    @Mock
    private ResourceUtils resourceUtils;

    @InjectMocks
    private FactBuilderService factBuilderService;

    private Map<String, Object> inputData;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        inputData = TestDataFactory.createInputData();
        when(resourceUtils.readResource(anyString())).thenReturn("");
    }

    // ========== buildCoreMetric ==========

    @Test
    public void testBuildCoreMetric_AllFieldsPopulated() {
        CoreMetricDto metric = factBuilderService.buildCoreMetric(inputData);

        assertNotNull(metric);
        assertEquals(85.5, metric.getScore(), 0.001);
        assertEquals(1.2, metric.getOffsetFactor(), 0.001);
        assertEquals(1000000.0, metric.getTotalAmount(), 0.001);
        assertEquals(Long.valueOf(5000L), metric.getCount());
        assertEquals(200.0, metric.getAvgAmount(), 0.001);
        assertEquals("2025-06-15", metric.getMetricDate());
        assertEquals("monthly", metric.getReportType());
    }

    @Test
    public void testBuildCoreMetric_MissingFields_ReturnsNullValues() {
        Map<String, Object> minimal = new LinkedHashMap<>();
        CoreMetricDto metric = factBuilderService.buildCoreMetric(minimal);

        assertNotNull(metric);
        assertNull("缺少 score 应为 null", metric.getScore());
        assertNull("缺少 offset_factor 应为 null", metric.getOffsetFactor());
        assertNull("缺少 count 应为 null", metric.getCount());
    }

    @Test
    public void testBuildCoreMetric_EmptyInput() {
        Map<String, Object> empty = new LinkedHashMap<>();
        CoreMetricDto metric = factBuilderService.buildCoreMetric(empty);

        assertNotNull(metric);
        assertNull(metric.getScore());
        assertNull(metric.getMetricDate());
    }

    // ========== buildDimensionScores ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildDimensionScores_SortedDescendingByScore() {
        List<DimensionScoreDto> scores = factBuilderService.buildDimensionScores(inputData);

        assertNotNull(scores);
        assertEquals(3, scores.size());

        // 验证按 score 降序排列: revenue(95) > users(88) > conversion(72)
        assertTrue(scores.get(0).getScore() >= scores.get(1).getScore());
        assertTrue(scores.get(1).getScore() >= scores.get(2).getScore());
        assertEquals("revenue", scores.get(0).getDimension());
        assertEquals("users", scores.get(1).getDimension());
        assertEquals("conversion", scores.get(2).getDimension());
    }

    @Test
    public void testBuildDimensionScores_IndividualFieldMapping() {
        List<DimensionScoreDto> scores = factBuilderService.buildDimensionScores(inputData);

        DimensionScoreDto first = scores.get(0);
        assertEquals(Double.valueOf(95.0), first.getScore());
        assertEquals(Double.valueOf(45.5), first.getContribution());
        assertEquals(Long.valueOf(3000L), first.getCount());
    }

    @Test
    public void testBuildDimensionScores_NoDimensionsKey() {
        Map<String, Object> noDims = new LinkedHashMap<>();
        noDims.put("score", 80);
        List<DimensionScoreDto> scores = factBuilderService.buildDimensionScores(noDims);

        assertNotNull(scores);
        assertTrue("无 dimensions 键时返回空列表", scores.isEmpty());
    }

    // ========== buildCrossSignals ==========

    @Test
    public void testBuildCrossSignals_AllFieldsMapped() {
        List<CrossSignalDto> signals = factBuilderService.buildCrossSignals(inputData);

        assertNotNull(signals);
        assertEquals(2, signals.size());

        CrossSignalDto s1 = signals.get(0);
        assertEquals("market_trend_up", s1.getSignalName());
        assertEquals(Double.valueOf(7.8), s1.getScore());
        assertEquals("up", s1.getTrend());
        assertEquals("市场整体呈上升趋势", s1.getDescription());
    }

    @Test
    public void testBuildCrossSignals_NoSignalsKey() {
        Map<String, Object> noSignals = new LinkedHashMap<>();
        noSignals.put("score", 80);

        List<CrossSignalDto> signals = factBuilderService.buildCrossSignals(noSignals);
        assertNotNull(signals);
        assertTrue(signals.isEmpty());
    }

    // ========== buildRootCauses ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildRootCauses_SortedByImpact() {
        List<RootCauseDto> causes = factBuilderService.buildRootCauses(inputData);

        assertNotNull(causes);
        assertEquals(3, causes.size());

        // 按 impact 降序: seasonality(35) > price_change(20) > competition(10)
        assertTrue(causes.get(0).getImpact() >= causes.get(1).getImpact());
        assertTrue(causes.get(1).getImpact() >= causes.get(2).getImpact());
        assertEquals("seasonality", causes.get(0).getDimension());
        assertEquals("price_change", causes.get(1).getDimension());
        assertEquals("competition", causes.get(2).getDimension());
    }

    @Test
    public void testBuildRootCauses_FieldMapping() {
        List<RootCauseDto> causes = factBuilderService.buildRootCauses(inputData);

        RootCauseDto first = causes.get(0);
        assertEquals("Q2 seasonal peak", first.getValue());
        assertEquals(Double.valueOf(35.0), first.getImpact());
        assertEquals(Double.valueOf(0.9), first.getConfidence());
        assertEquals("Q2 is historically the strongest quarter", first.getDescription());
    }

    // ========== buildOffsetFactor ==========

    @Test
    public void testBuildOffsetFactor_AllFields() {
        OffsetFactorDto offset = factBuilderService.buildOffsetFactor(inputData);

        assertNotNull(offset);
        assertEquals(Double.valueOf(1.05), offset.getTotalFactor(), 0.001);
        assertEquals(Double.valueOf(0.98), offset.getDayOfWeekFactor(), 0.001);
        assertEquals(Double.valueOf(1.02), offset.getHolidayFactor(), 0.001);
        assertEquals(Double.valueOf(1.10), offset.getTrendFactor(), 0.001);
        assertEquals(Double.valueOf(0.95), offset.getPromotionFactor(), 0.001);
    }

    // ========== buildSegmentMetric ==========

    @Test
    public void testBuildSegmentMetric_FieldsMapped() {
        SegmentMetricDto segment = factBuilderService.buildSegmentMetric(inputData);

        assertNotNull(segment);
        assertEquals("premium_customers", segment.getSegmentName());
        assertEquals(Double.valueOf(92.0), segment.getScore(), 0.001);
        assertEquals(Double.valueOf(500000.0), segment.getAmount(), 0.001);
        assertEquals(Long.valueOf(1000L), segment.getCount());
        assertEquals(Double.valueOf(0.15), segment.getConversionRate(), 0.001);
    }

    // ========== buildFactPayload ==========

    @Test
    public void testBuildFactPayload_StructureComplete() {
        CoreMetricDto core = TestDataFactory.createCoreMetric();
        List<DimensionScoreDto> dims = TestDataFactory.createDimensionScores();
        List<CrossSignalDto> sigs = TestDataFactory.createCrossSignals();

        Map<String, Object> payload = factBuilderService.buildFactPayload(core, dims, sigs, "growth");

        assertNotNull(payload);
        assertEquals("growth", payload.get("scenario"));
        assertNotNull(payload.get("core_metric"));
        assertNotNull(payload.get("dimensions"));
        assertNotNull(payload.get("cross_signals"));
        assertNotNull(payload.get("timestamp"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildFactPayload_PrecisionRounding() {
        CoreMetricDto core = TestDataFactory.createCoreMetric();
        List<DimensionScoreDto> dims = TestDataFactory.createDimensionScores();
        List<CrossSignalDto> sigs = TestDataFactory.createCrossSignals();

        Map<String, Object> payload = factBuilderService.buildFactPayload(core, dims, sigs, "test");

        // 验证 core_metric 内部精度
        Map<String, Object> cm = (Map<String, Object>) payload.get("core_metric");
        // score/offset_factor 应保留 6 位小数，total_amount/avg_amount 应保留 2 位
        Object scoreVal = cm.get("score");
        if (scoreVal instanceof Number) {
            double score = ((Number) scoreVal).doubleValue();
            String scoreStr = String.valueOf(score);
            int decimals = scoreStr.contains(".") ? scoreStr.split("\\.")[1].length() : 0;
            assertTrue("score 应保留 <=6 位小数", decimals <= 6);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBuildFactPayload_DimensionsPreserveOrder() {
        CoreMetricDto core = TestDataFactory.createCoreMetric();
        List<DimensionScoreDto> dims = TestDataFactory.createDimensionScores();
        List<CrossSignalDto> sigs = TestDataFactory.createCrossSignals();

        Map<String, Object> payload = factBuilderService.buildFactPayload(core, dims, sigs, "test");
        List<Map<String, Object>> dimList = (List<Map<String, Object>>) payload.get("dimensions");

        assertEquals(3, dimList.size());
        assertEquals("revenue", dimList.get(0).get("dimension"));
        assertEquals("users", dimList.get(1).get("dimension"));
        assertEquals("conversion", dimList.get(2).get("dimension"));
    }

    // ========== loadScenarioDefault ==========

    @Test
    public void testLoadScenarioDefault_FileExists_ParsesJson() {
        ScenarioDefaultDto expected = new ScenarioDefaultDto();
        expected.setScenario("growth");
        expected.setDefaultScore(75.0);
        expected.setDefaultOffsetFactor(1.1);

        when(resourceUtils.readResource(anyString()))
                .thenReturn("{\"scenario\":\"growth\",\"default_score\":75.0,\"default_offset_factor\":1.1}");
        when(jsonUtils.fromJson(anyString(), eq(ScenarioDefaultDto.class))).thenReturn(expected);

        ScenarioDefaultDto result = factBuilderService.loadScenarioDefault("growth");

        assertNotNull(result);
        assertEquals("growth", result.getScenario());
        verify(resourceUtils).readResource("data/scenario_default_growth.json");
    }

    @Test
    public void testLoadScenarioDefault_FileNotFound_ReturnsDefault() {
        when(resourceUtils.readResource(anyString())).thenReturn("");

        ScenarioDefaultDto result = factBuilderService.loadScenarioDefault("nonexistent");

        assertNotNull(result);
        assertEquals("nonexistent", result.getScenario());
        assertEquals(Double.valueOf(0.0), result.getDefaultScore());
        assertEquals(Double.valueOf(1.0), result.getDefaultOffsetFactor());
        assertEquals("暂无洞察数据", result.getDefaultInsight());
    }

    @Test
    public void testLoadScenarioDefault_InvalidJson_ReturnsDefault() {
        when(resourceUtils.readResource(anyString())).thenReturn("not valid json {}");
        when(jsonUtils.fromJson(anyString(), eq(ScenarioDefaultDto.class)))
                .thenThrow(new RuntimeException("Parse error"));

        ScenarioDefaultDto result = factBuilderService.loadScenarioDefault("broken");

        assertNotNull(result);
        assertEquals("broken", result.getScenario());
        assertEquals(0.0, result.getDefaultScore(), 0.001);
    }

    // ========== loadCardConfig ==========

    @Test
    public void testLoadCardConfig_CardFoundInConfig() {
        CardConfigDto expected = new CardConfigDto();
        expected.setCardId("card_001");
        expected.setCardName("Revenue Overview");

        Map<String, Object> configMap = new LinkedHashMap<>();
        Map<String, Object> cardMap = new LinkedHashMap<>();
        cardMap.put("card_id", "card_001");
        cardMap.put("card_name", "Revenue Overview");
        configMap.put("card_001", cardMap);

        when(resourceUtils.readResource("data/card_config.json"))
                .thenReturn("{\"card_001\":{\"card_id\":\"card_001\",\"card_name\":\"Revenue Overview\"}}");
        when(jsonUtils.fromJson(anyString(), eq(Map.class))).thenReturn(configMap);
        when(jsonUtils.fromJson(anyString(), eq(CardConfigDto.class))).thenReturn(expected);

        CardConfigDto result = factBuilderService.loadCardConfig("card_001");

        assertNotNull(result);
        assertEquals("card_001", result.getCardId());
        assertEquals("Revenue Overview", result.getCardName());
    }

    @Test
    public void testLoadCardConfig_CardNotFoundInConfig_ReturnsDefault() {
        when(resourceUtils.readResource("data/card_config.json"))
                .thenReturn("{\"other_card\":{\"card_id\":\"other\"}}");
        when(jsonUtils.fromJson(anyString(), eq(Map.class)))
                .thenAnswer(inv -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("other_card", new LinkedHashMap<>());
                    return m;
                });

        CardConfigDto result = factBuilderService.loadCardConfig("missing_card");

        assertNotNull(result);
        assertEquals("missing_card", result.getCardId());
        assertEquals("未知卡片", result.getCardName());
        assertEquals(true, result.isEnabled());
    }

    @Test
    public void testLoadCardConfig_ConfigFileEmpty_ReturnsDefault() {
        when(resourceUtils.readResource("data/card_config.json")).thenReturn("");

        CardConfigDto result = factBuilderService.loadCardConfig("any_id");

        assertNotNull(result);
        assertEquals("未知卡片", result.getCardName());
        assertEquals(Double.valueOf(0.5), result.getThreshold(), 0.001);
    }

    // ========== 数值类型转换边界测试 ==========

    @Test
    public void testBuildCoreMetric_IntegerValue_ConvertedToDouble() {
        Map<String, Object> intInput = new LinkedHashMap<>();
        intInput.put("score", 85);   // Integer 非 Double
        intInput.put("count", 5000); // Integer
        intInput.put("total_amount", 1000000L); // Long

        CoreMetricDto metric = factBuilderService.buildCoreMetric(intInput);

        assertNotNull(metric);
        assertEquals(85.0, metric.getScore(), 0.001);
        assertEquals(Long.valueOf(5000L), metric.getCount());
        assertEquals(1000000.0, metric.getTotalAmount(), 0.001);
    }
}
