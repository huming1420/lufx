package com.lufax.dashboard.service;

import com.lufax.dashboard.model.dto.*;
import com.lufax.dashboard.model.entity.InsightResult;
import com.lufax.dashboard.util.HashUtils;
import com.lufax.dashboard.util.JsonUtils;
import com.lufax.dashboard.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RuleEngineService {

    private static final Logger logger = LoggerFactory.getLogger(RuleEngineService.class);

    private final Map<String, RulesDto> rulesCache = new ConcurrentHashMap<>();

    @Autowired
    private JsonUtils jsonUtils;

    @Autowired
    private ResourceUtils resourceUtils;

    @Autowired
    private HashUtils hashUtils;

    public RulesDto loadRules(String scenario) {
        String cacheKey = scenario;
        RulesDto cached = rulesCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        String rulesPath = String.format("rules/%s_rules.json", scenario);
        String content = resourceUtils.readResource(rulesPath);
        if (content == null || content.isEmpty()) {
            logger.warn("Rules file not found: {}", rulesPath);
            return createDefaultRules();
        }

        try {
            RulesDto rules = jsonUtils.fromJson(content, RulesDto.class);
            rulesCache.put(cacheKey, rules);
            return rules;
        } catch (Exception e) {
            logger.error("Failed to load rules for scenario {}", scenario, e);
            return createDefaultRules();
        }
    }

    private RulesDto createDefaultRules() {
        RulesDto rules = new RulesDto();
        rules.setRules(new ArrayList<>());
        rules.setScoreThreshold(0.5);
        rules.setVersion("1.0");
        return rules;
    }

    public Map<String, Object> evaluate(CoreMetricDto coreMetric, List<DimensionScoreDto> dimensionScores,
                                        List<CrossSignalDto> crossSignals, RulesDto rules) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> triggeredRules = new ArrayList<>();
        double totalScore = 0.0;

        for (Object ruleObj : rules.getRules()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleObj;
            if (evaluateRule(rule, coreMetric, dimensionScores, crossSignals)) {
                triggeredRules.add(rule);
                totalScore += getRuleScore(rule);
            }
        }

        result.put("triggered_rules", triggeredRules);
        result.put("total_score", totalScore);
        result.put("is_alert", totalScore >= rules.getScoreThreshold());

        return result;
    }

    private boolean evaluateRule(Map<String, Object> rule, CoreMetricDto coreMetric,
                                 List<DimensionScoreDto> dimensionScores, List<CrossSignalDto> crossSignals) {
        Object conditionsObj = rule.get("conditions");
        if (!(conditionsObj instanceof List)) {
            return false;
        }

        List<Map<String, Object>> conditions = (List<Map<String, Object>>) conditionsObj;
        for (Map<String, Object> condition : conditions) {
            if (!evaluateCondition(condition, coreMetric, dimensionScores, crossSignals)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateCondition(Map<String, Object> condition, CoreMetricDto coreMetric,
                                     List<DimensionScoreDto> dimensionScores, List<CrossSignalDto> crossSignals) {
        String type = (String) condition.get("type");
        String field = (String) condition.get("field");
        String operator = (String) condition.get("operator");
        Object value = condition.get("value");

        switch (type) {
            case "core_metric":
                return evaluateCoreMetricCondition(coreMetric, field, operator, value);
            case "dimension":
                return evaluateDimensionCondition(dimensionScores, field, operator, value);
            case "cross_signal":
                return evaluateCrossSignalCondition(crossSignals, field, operator, value);
            default:
                return false;
        }
    }

    private boolean evaluateCoreMetricCondition(CoreMetricDto coreMetric, String field, String operator, Object value) {
        Double metricValue = getCoreMetricValue(coreMetric, field);
        if (metricValue == null) return false;

        Double threshold = toDouble(value);
        if (threshold == null) return false;

        return compare(metricValue, operator, threshold);
    }

    private boolean evaluateDimensionCondition(List<DimensionScoreDto> dimensionScores, String field,
                                               String operator, Object value) {
        Double threshold = toDouble(value);
        if (threshold == null) return false;

        for (DimensionScoreDto dim : dimensionScores) {
            if (field.equals(dim.getDimension())) {
                return compare(dim.getScore(), operator, threshold);
            }
        }
        return false;
    }

    private boolean evaluateCrossSignalCondition(List<CrossSignalDto> crossSignals, String field,
                                                 String operator, Object value) {
        Double threshold = toDouble(value);
        if (threshold == null) return false;

        for (CrossSignalDto signal : crossSignals) {
            if (field.equals(signal.getSignalName())) {
                return compare(signal.getScore(), operator, threshold);
            }
        }
        return false;
    }

    private Double getCoreMetricValue(CoreMetricDto coreMetric, String field) {
        switch (field) {
            case "score":
                return coreMetric.getScore();
            case "offset_factor":
                return coreMetric.getOffsetFactor();
            case "total_amount":
                return coreMetric.getTotalAmount();
            case "count":
                return coreMetric.getCount() != null ? coreMetric.getCount().doubleValue() : null;
            case "avg_amount":
                return coreMetric.getAvgAmount();
            default:
                return null;
        }
    }

    private boolean compare(Double actual, String operator, Double expected) {
        if (actual == null || expected == null) return false;

        switch (operator) {
            case ">":
                return actual > expected;
            case ">=":
                return actual >= expected;
            case "<":
                return actual < expected;
            case "<=":
                return actual <= expected;
            case "==":
            case "=":
                return Math.abs(actual - expected) < 1e-9;
            case "!=":
                return Math.abs(actual - expected) >= 1e-9;
            default:
                return false;
        }
    }

    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private double getRuleScore(Map<String, Object> rule) {
        Object scoreObj = rule.get("score");
        if (scoreObj instanceof Number) {
            return ((Number) scoreObj).doubleValue();
        }
        return 1.0;
    }

    public String computeScenarioHash(String scenario, CoreMetricDto coreMetric,
                                      List<DimensionScoreDto> dimensionScores) {
        Map<String, Object> hashInput = new LinkedHashMap<>();
        hashInput.put("scenario", scenario);
        hashInput.put("score", coreMetric.getScore());
        hashInput.put("offset_factor", coreMetric.getOffsetFactor());
        hashInput.put("total_amount", coreMetric.getTotalAmount());
        hashInput.put("count", coreMetric.getCount());

        List<Map<String, Object>> dimList = new ArrayList<>();
        for (DimensionScoreDto dim : dimensionScores) {
            Map<String, Object> dimMap = new LinkedHashMap<>();
            dimMap.put("dimension", dim.getDimension());
            dimMap.put("score", dim.getScore());
            dimList.add(dimMap);
        }
        hashInput.put("dimensions", dimList);

        return hashUtils.computeSha256(jsonUtils.toJson(hashInput));
    }

    public String getScenarioVersion(InsightResult result) {
        String cacheKey = result.getScenario() + ":" + result.getScenarioHash();
        return hashUtils.computeSha1(cacheKey);
    }
}