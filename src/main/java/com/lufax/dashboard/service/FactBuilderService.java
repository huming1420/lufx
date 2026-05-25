package com.lufax.dashboard.service;

import com.lufax.dashboard.model.dto.*;
import com.lufax.dashboard.model.entity.InsightResult;
import com.lufax.dashboard.util.JsonUtils;
import com.lufax.dashboard.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class FactBuilderService {

    private static final Logger logger = LoggerFactory.getLogger(FactBuilderService.class);

    @Autowired
    private JsonUtils jsonUtils;

    @Autowired
    private ResourceUtils resourceUtils;

    public CoreMetricDto buildCoreMetric(Map<String, Object> inputData) {
        CoreMetricDto metric = new CoreMetricDto();

        metric.setScore(getDoubleValue(inputData, "score"));
        metric.setOffsetFactor(getDoubleValue(inputData, "offset_factor"));
        metric.setTotalAmount(getDoubleValue(inputData, "total_amount"));
        metric.setCount(getLongValue(inputData, "count"));
        metric.setAvgAmount(getDoubleValue(inputData, "avg_amount"));
        metric.setMetricDate((String) inputData.get("metric_date"));
        metric.setReportType((String) inputData.get("report_type"));

        return metric;
    }

    public List<DimensionScoreDto> buildDimensionScores(Map<String, Object> inputData) {
        List<DimensionScoreDto> scores = new ArrayList<>();
        Object dimensionsObj = inputData.get("dimensions");

        if (dimensionsObj instanceof List) {
            List<Map<String, Object>> dimensions = (List<Map<String, Object>>) dimensionsObj;
            for (Map<String, Object> dim : dimensions) {
                DimensionScoreDto score = new DimensionScoreDto();
                score.setDimension((String) dim.get("dimension"));
                score.setScore(getDoubleValue(dim, "score"));
                score.setContribution(getDoubleValue(dim, "contribution"));
                score.setCount(getLongValue(dim, "count"));
                scores.add(score);
            }
        }

        scores.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return scores;
    }

    public List<CrossSignalDto> buildCrossSignals(Map<String, Object> inputData) {
        List<CrossSignalDto> signals = new ArrayList<>();
        Object signalsObj = inputData.get("cross_signals");

        if (signalsObj instanceof List) {
            List<Map<String, Object>> signalList = (List<Map<String, Object>>) signalsObj;
            for (Map<String, Object> signal : signalList) {
                CrossSignalDto dto = new CrossSignalDto();
                dto.setSignalName((String) signal.get("signal_name"));
                dto.setScore(getDoubleValue(signal, "score"));
                dto.setTrend((String) signal.get("trend"));
                dto.setDescription((String) signal.get("description"));
                signals.add(dto);
            }
        }

        return signals;
    }

    public OffsetFactorDto buildOffsetFactor(Map<String, Object> inputData) {
        OffsetFactorDto offset = new OffsetFactorDto();

        offset.setTotalFactor(getDoubleValue(inputData, "total_factor"));
        offset.setDayOfWeekFactor(getDoubleValue(inputData, "day_of_week_factor"));
        offset.setHolidayFactor(getDoubleValue(inputData, "holiday_factor"));
        offset.setTrendFactor(getDoubleValue(inputData, "trend_factor"));
        offset.setPromotionFactor(getDoubleValue(inputData, "promotion_factor"));

        return offset;
    }

    public SegmentMetricDto buildSegmentMetric(Map<String, Object> inputData) {
        SegmentMetricDto segment = new SegmentMetricDto();

        segment.setSegmentName((String) inputData.get("segment_name"));
        segment.setScore(getDoubleValue(inputData, "score"));
        segment.setAmount(getDoubleValue(inputData, "amount"));
        segment.setCount(getLongValue(inputData, "count"));
        segment.setConversionRate(getDoubleValue(inputData, "conversion_rate"));

        return segment;
    }

    public List<RootCauseDto> buildRootCauses(Map<String, Object> inputData) {
        List<RootCauseDto> rootCauses = new ArrayList<>();
        Object rootCausesObj = inputData.get("root_causes");

        if (rootCausesObj instanceof List) {
            List<Map<String, Object>> causes = (List<Map<String, Object>>) rootCausesObj;
            for (Map<String, Object> cause : causes) {
                RootCauseDto dto = new RootCauseDto();
                dto.setDimension((String) cause.get("dimension"));
                dto.setValue((String) cause.get("value"));
                dto.setImpact(getDoubleValue(cause, "impact"));
                dto.setConfidence(getDoubleValue(cause, "confidence"));
                dto.setDescription((String) cause.get("description"));
                rootCauses.add(dto);
            }
        }

        rootCauses.sort((a, b) -> Double.compare(b.getImpact(), a.getImpact()));
        return rootCauses;
    }

    public Map<String, Object> buildFactPayload(CoreMetricDto coreMetric, List<DimensionScoreDto> dimensionScores,
                                                List<CrossSignalDto> crossSignals, String scenario) {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("scenario", scenario);
        payload.put("metric_date", coreMetric.getMetricDate());
        payload.put("report_type", coreMetric.getReportType());

        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("score", roundTo6Decimals(coreMetric.getScore()));
        metric.put("offset_factor", roundTo6Decimals(coreMetric.getOffsetFactor()));
        metric.put("total_amount", roundTo2Decimals(coreMetric.getTotalAmount()));
        metric.put("count", coreMetric.getCount());
        metric.put("avg_amount", roundTo2Decimals(coreMetric.getAvgAmount()));
        payload.put("core_metric", metric);

        List<Map<String, Object>> dimensions = new ArrayList<>();
        for (DimensionScoreDto dim : dimensionScores) {
            Map<String, Object> dimMap = new LinkedHashMap<>();
            dimMap.put("dimension", dim.getDimension());
            dimMap.put("score", roundTo6Decimals(dim.getScore()));
            dimMap.put("contribution", roundTo6Decimals(dim.getContribution()));
            dimMap.put("count", dim.getCount());
            dimensions.add(dimMap);
        }
        payload.put("dimensions", dimensions);

        List<Map<String, Object>> signals = new ArrayList<>();
        for (CrossSignalDto signal : crossSignals) {
            Map<String, Object> signalMap = new LinkedHashMap<>();
            signalMap.put("signal_name", signal.getSignalName());
            signalMap.put("score", roundTo6Decimals(signal.getScore()));
            signalMap.put("trend", signal.getTrend());
            signalMap.put("description", signal.getDescription());
            signals.add(signalMap);
        }
        payload.put("cross_signals", signals);

        payload.put("timestamp", System.currentTimeMillis());

        return payload;
    }

    public ScenarioDefaultDto loadScenarioDefault(String scenario) {
        String path = String.format("data/scenario_default_%s.json", scenario);
        String content = resourceUtils.readResource(path);
        if (content == null || content.isEmpty()) {
            return createDefaultScenario(scenario);
        }

        try {
            return jsonUtils.fromJson(content, ScenarioDefaultDto.class);
        } catch (Exception e) {
            logger.warn("Failed to load scenario default for {}: {}", scenario, e.getMessage());
            return createDefaultScenario(scenario);
        }
    }

    private ScenarioDefaultDto createDefaultScenario(String scenario) {
        ScenarioDefaultDto dto = new ScenarioDefaultDto();
        dto.setScenario(scenario);
        dto.setDefaultScore(0.0);
        dto.setDefaultOffsetFactor(1.0);
        dto.setDefaultInsight("暂无洞察数据");
        return dto;
    }

    public CardConfigDto loadCardConfig(String cardId) {
        String content = resourceUtils.readResource("data/card_config.json");
        if (content == null || content.isEmpty()) {
            return createDefaultCardConfig(cardId);
        }

        try {
            Map<String, Object> configMap = jsonUtils.fromJson(content, Map.class);
            Object cardObj = configMap.get(cardId);
            if (cardObj != null) {
                String cardJson = jsonUtils.toJson(cardObj);
                return jsonUtils.fromJson(cardJson, CardConfigDto.class);
            }
        } catch (Exception e) {
            logger.warn("Failed to load card config for {}: {}", cardId, e.getMessage());
        }

        return createDefaultCardConfig(cardId);
    }

    private CardConfigDto createDefaultCardConfig(String cardId) {
        CardConfigDto config = new CardConfigDto();
        config.setCardId(cardId);
        config.setCardName("未知卡片");
        config.setDescription("暂无描述");
        config.setThreshold(0.5);
        config.setEnabled(true);
        return config;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Double roundTo2Decimals(Double value) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Double roundTo6Decimals(Double value) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }
}