package com.lufax.dashboard.service;

import com.lufax.dashboard.llm.LlmAdapter;
import com.lufax.dashboard.model.dto.CoreMetricDto;
import com.lufax.dashboard.model.dto.DimensionScoreDto;
import com.lufax.dashboard.model.entity.InsightResult;
import com.lufax.dashboard.model.entity.MetricSnapshot;
import com.lufax.dashboard.model.request.CardInsightRequest;
import com.lufax.dashboard.model.request.DashboardInsightRequest;
import com.lufax.dashboard.model.request.ScenarioInsightRequest;
import com.lufax.dashboard.repository.InsightResultMapper;
import com.lufax.dashboard.util.HashUtils;
import com.lufax.dashboard.util.JsonUtils;
import com.lufax.dashboard.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InsightService {
    private static final Logger logger = LoggerFactory.getLogger(InsightService.class);

    @Autowired
    private LlmAdapter llmAdapter;

    @Autowired
    private FactPackService factPackService;

    @Autowired
    private InsightResultMapper insightResultMapper;

    @Value("${app.default-period:2026-03-YTD}")
    private String defaultPeriod;

    @Value("${app.prompt-version:v2}")
    private String promptVersion;

    @Transactional
    public Map<String, Object> generateCardInsight(CardInsightRequest request) {
        String period = period(request.getPeriod());
        String cardId = request.getCardId();
        return generate("card", cardId, cardId, period,
                factPackService.buildCardFactPack(period, cardId), "card_insight_v2.txt");
    }

    @Transactional
    public Map<String, Object> generateDashboardInsight(DashboardInsightRequest request) {
        String period = period(request.getPeriod());
        return generate("dashboard", "", "", period,
                factPackService.buildDashboardFactPack(period), "dashboard_insight_v2.txt");
    }

    @Transactional
    public Map<String, Object> generateScenarioInsight(ScenarioInsightRequest request) {
        String period = period(request.getPeriod());
        String scenario = firstNonBlank(request.getScenario(), request.getScenarioId(), "base");
        Map<String, Object> factPack =
                factPackService.buildScenarioFactPack(period, scenario, request.getInputs());
        String factHash = HashUtils.sha256(JsonUtils.toSortedJson(factPack));
        String cacheId = scenario + ":" + factHash.substring(0, 16);
        return generate("scenario", cacheId, scenario, period, factPack,
                "scenario_insight_v2.txt");
    }

    protected Map<String, Object> generate(String type, String cacheId, String businessId, String period,
                                           Map<String, Object> factPack, String promptResource) {
        MetricSnapshot snapshot = factPackService.activeSnapshot(period);
        InsightResult cached = insightResultMapper.selectByCacheKey(
                period, snapshot.getMetricVersion(), promptVersion, type, cacheId);
        if (cached != null && "ready".equals(cached.getStatus())) {
            return JsonUtils.toMap(cached.getResultJson());
        }

        try {
            String rawFactPack = JsonUtils.toJson(factPack);
            String rawResponse = llmAdapter.generate(
                    ResourceUtils.readClasspathFileUtf8("prompts/" + promptResource), factPack);
            Map<String, Object> result = JsonUtils.loadsJsonObject(rawResponse);
            validateAiOwnedFields(type, result);
            result.put("status", "ready");
            result.put("period", period);
            result.put("metric_version", snapshot.getMetricVersion());
            result.put("data_source", factPack.get("data_source"));
            if ("card".equals(type)) {
                result.put("card_id", businessId);
            } else if ("scenario".equals(type)) {
                result.put("scenario", businessId);
            }
            persistResult(type, cacheId, businessId, snapshot, factPack, rawFactPack, rawResponse, result);
            return result;
        } catch (Exception e) {
            logger.error("AI insight generation failed for {} / {}", type, cacheId, e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", "failed");
            error.put("period", period);
            error.put("error", e.getMessage());
            error.put("data_source", factPack.get("data_source"));
            return error;
        }
    }

    protected void persistResult(String type, String cacheId, String businessId, MetricSnapshot snapshot,
                                 Map<String, Object> factPack, String rawFactPack,
                                 String rawResponse, Map<String, Object> result) {
        InsightResult record = new InsightResult();
        record.setPeriod(snapshot.getPeriod());
        record.setMetricVersion(snapshot.getMetricVersion());
        record.setPromptVersion(promptVersion);
        record.setInsightType(type);
        record.setCardId(cacheId);
        record.setScenario("scenario".equals(type) ? businessId : null);
        record.setStatus("ready");
        record.setSchemaName(type + "_insight.schema.json");
        record.setRawFactPackJson(rawFactPack);
        record.setRawLlmOutput(rawResponse);
        record.setRawLlmOutputJson(rawResponse);
        record.setResultJson(JsonUtils.toJson(result));
        record.setTrafficLight(String.valueOf(result.get(
                "dashboard".equals(type) ? "overall_traffic_light" : "traffic_light")));
        record.setAnalysisLabelsJson(JsonUtils.toJson(result.get("analysis_labels")));
        record.setValidated(true);
        record.setScenarioHash(HashUtils.sha256(JsonUtils.toSortedJson(factPack)));
        insightResultMapper.insertOrUpdate(record);
    }

    private void validateAiOwnedFields(String type, Map<String, Object> result) {
        if (result == null || result.isEmpty()) {
            throw new IllegalStateException("Model returned no JSON result");
        }
        String lightField = "dashboard".equals(type) ? "overall_traffic_light" : "traffic_light";
        if (!result.containsKey(lightField) || !result.containsKey("analysis_labels")) {
            throw new IllegalStateException("Model response must supply " + lightField + " and analysis_labels");
        }
        if ("card".equals(type) && !result.containsKey("analysis")) {
            throw new IllegalStateException("Model card response lacks analysis");
        }
        if ("scenario".equals(type) && !result.containsKey("standard_explanation")) {
            throw new IllegalStateException("Model scenario response lacks standard_explanation");
        }
    }

    public InsightResult getInsightById(Long id) {
        return insightResultMapper.selectById(id);
    }

    public List<InsightResult> getInsightsByScenario(String scenario) {
        return insightResultMapper.selectByScenario(scenario);
    }

    public List<InsightResult> getInsightsByScenarioAndDate(String scenario, String metricDate) {
        return insightResultMapper.selectByScenarioAndDate(scenario, metricDate);
    }

    /**
     * Kept for legacy callers while standard generation now persists structured database-backed results.
     */
    public void saveInsightResult(String scenario, String metricDate, Map<String, Object> insightData,
                                  CoreMetricDto coreMetric, List<DimensionScoreDto> dimensions) {
        logger.warn("Legacy saveInsightResult is deprecated; use database-backed insight generation");
    }

    private String period(String value) {
        return value == null || value.trim().isEmpty() ? defaultPeriod : value;
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return fallback;
    }
}
