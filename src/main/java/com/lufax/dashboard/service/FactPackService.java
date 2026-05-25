package com.lufax.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lufax.dashboard.model.entity.BusinessMetric;
import com.lufax.dashboard.model.entity.CardDefinition;
import com.lufax.dashboard.model.entity.MetricSnapshot;
import com.lufax.dashboard.model.entity.ScenarioBaseline;
import com.lufax.dashboard.repository.BusinessFactMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FactPackService {

    @Autowired
    private BusinessFactMapper mapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> buildCardFactPack(String period, String cardId) {
        MetricSnapshot snapshot = requireSnapshot(period);
        Map<String, Object> pack = basePack("card", snapshot);
        pack.put("card_id", cardId);
        for (CardDefinition card : mapper.selectCards()) {
            if (cardId.equals(card.getCardId())) {
                pack.put("card_title", card.getTitle());
                pack.put("module", card.getModule());
                break;
            }
        }
        pack.put("metrics", businessFacts(
                mapper.selectMetricsForCard(period, snapshot.getMetricVersion(), cardId)));
        return pack;
    }

    public Map<String, Object> buildDashboardFactPack(String period) {
        MetricSnapshot snapshot = requireSnapshot(period);
        Map<String, Object> pack = basePack("dashboard", snapshot);
        List<Map<String, Object>> core = new ArrayList<>();
        List<Map<String, Object>> segment = new ArrayList<>();
        for (BusinessMetric metric : mapper.selectMetrics(period, snapshot.getMetricVersion())) {
            if ("core".equals(metric.getMetricScope())) {
                core.add(businessFact(metric));
            } else {
                segment.add(businessFact(metric));
            }
        }
        pack.put("core_metrics", core);
        pack.put("segment_metrics", segment);
        return pack;
    }

    public Map<String, Object> buildScenarioFactPack(String period, String scenario,
                                                     Map<String, Object> overrides) {
        MetricSnapshot snapshot = requireSnapshot(period);
        ScenarioBaseline baseline = mapper.selectScenarioBaseline(period, snapshot.getMetricVersion(), scenario);
        if (baseline == null) {
            throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }
        Map<String, Object> pack = basePack("scenario", snapshot);
        Map<String, Object> inputs = parseInputs(baseline.getInputsJson());
        if (overrides != null) {
            inputs.putAll(overrides);
        }
        pack.put("scenario", scenario);
        pack.put("scenario_label", baseline.getScenarioLabel());
        pack.put("inputs", inputs);
        pack.put("core_metrics", businessFacts(mapper.selectMetrics(period, snapshot.getMetricVersion())));
        return pack;
    }

    public MetricSnapshot activeSnapshot(String period) {
        return requireSnapshot(period);
    }

    public List<BusinessMetric> metrics(String period) {
        MetricSnapshot snapshot = requireSnapshot(period);
        return mapper.selectMetrics(period, snapshot.getMetricVersion());
    }

    public List<CardDefinition> cards() {
        return mapper.selectCards();
    }

    public List<ScenarioBaseline> scenarios(String period) {
        MetricSnapshot snapshot = requireSnapshot(period);
        return mapper.selectScenarioBaselines(period, snapshot.getMetricVersion());
    }

    private MetricSnapshot requireSnapshot(String period) {
        MetricSnapshot snapshot = mapper.selectActiveSnapshot(period);
        if (snapshot == null) {
            throw new IllegalStateException("No metric snapshot available for period " + period);
        }
        return snapshot;
    }

    private Map<String, Object> basePack(String type, MetricSnapshot snapshot) {
        Map<String, Object> pack = new LinkedHashMap<>();
        pack.put("pack_type", type);
        pack.put("period", snapshot.getPeriod());
        pack.put("metric_version", snapshot.getMetricVersion());
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", snapshot.getSourceType());
        source.put("label", snapshot.getSourceLabel());
        source.put("file", snapshot.getSourceFile());
        pack.put("data_source", source);
        return pack;
    }

    private List<Map<String, Object>> businessFacts(List<BusinessMetric> metrics) {
        List<Map<String, Object>> facts = new ArrayList<>();
        for (BusinessMetric metric : metrics) {
            facts.add(businessFact(metric));
        }
        return facts;
    }

    private Map<String, Object> businessFact(BusinessMetric metric) {
        Map<String, Object> fact = new LinkedHashMap<>();
        fact.put("metric_scope", metric.getMetricScope());
        fact.put("metric_code", metric.getMetricCode());
        fact.put("metric_name", metric.getMetricName());
        fact.put("module", metric.getModule());
        fact.put("segment", metric.getSegment());
        fact.put("value", metric.getValue());
        fact.put("display_value", metric.getDisplayValue());
        fact.put("unit", metric.getUnit());
        fact.put("yoy", metric.getYoy());
        fact.put("display_yoy", metric.getDisplayYoy());
        fact.put("mom", metric.getMom());
        fact.put("budget", metric.getBudget());
        fact.put("budget_gap", metric.getBudgetGap());
        fact.put("display_budget_gap", metric.getDisplayBudgetGap());
        fact.put("redline", metric.getRedline());
        fact.put("yellowline", metric.getYellowline());
        fact.put("distance_to_redline", metric.getDistanceToRedline());
        fact.put("display_distance_to_redline", metric.getDisplayDistanceToRedline());
        fact.put("direction", metric.getDirection());
        fact.put("product_type", metric.getProductType());
        fact.put("channel_type", metric.getChannelType());
        fact.put("customer_type", metric.getCustomerType());
        fact.put("vintage", metric.getVintage());
        fact.put("mob", metric.getMob());
        return fact;
    }

    private Map<String, Object> parseInputs(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
        } catch (IOException e) {
            throw new IllegalStateException("Invalid stored scenario inputs", e);
        }
    }
}
