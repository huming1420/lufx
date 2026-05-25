package com.lufax.dashboard.controller;

import com.lufax.dashboard.model.dto.CardConfigDto;
import com.lufax.dashboard.model.dto.CoreMetricDto;
import com.lufax.dashboard.model.dto.ScenarioDefaultDto;
import com.lufax.dashboard.model.dto.SegmentMetricDto;
import com.lufax.dashboard.model.entity.BusinessMetric;
import com.lufax.dashboard.model.entity.CardDefinition;
import com.lufax.dashboard.model.entity.InsightResult;
import com.lufax.dashboard.model.entity.MetricSnapshot;
import com.lufax.dashboard.model.entity.ScenarioBaseline;
import com.lufax.dashboard.model.request.DashboardInsightRequest;
import com.lufax.dashboard.model.response.DashboardDataResponse;
import com.lufax.dashboard.model.response.HealthResponse;
import com.lufax.dashboard.service.FactPackService;
import com.lufax.dashboard.service.InsightService;
import com.lufax.dashboard.util.JsonUtils;
import com.lufax.dashboard.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private InsightService insightService;

    @Autowired
    private FactPackService factPackService;

    @Autowired
    private ResourceUtils resourceUtils;

    @Autowired
    private JsonUtils jsonUtils;

    @Value("${app.default-period:2026-03-YTD}")
    private String defaultPeriod;

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse();
        response.setStatus("UP");
        response.setService("dashboard-backend");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/dashboard/insight")
    public ResponseEntity<Map<String, Object>> dashboardInsight(@RequestBody DashboardInsightRequest request) {
        logger.info("Received dashboard insight request for scenario: {}", request.getScenario());
        Map<String, Object> result = insightService.generateDashboardInsight(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/dashboard/data")
    public ResponseEntity<DashboardDataResponse> dashboardData(
            @RequestParam(defaultValue = "base") String scenario,
            @RequestParam(required = false) String metric_date) {
        logger.info("Received dashboard data request for scenario: {}, date: {}", scenario, metric_date);

        String period = metric_date == null || metric_date.trim().isEmpty() ? defaultPeriod : metric_date;
        MetricSnapshot snapshot = factPackService.activeSnapshot(period);
        DashboardDataResponse response = new DashboardDataResponse();
        response.setPeriod(period);
        response.setMetricVersion(snapshot.getMetricVersion());
        response.setDataSource(sourceOf(snapshot));
        response.setScenario(scenario);
        response.setMetricDate(metric_date);

        List<CoreMetricDto> coreMetrics = new ArrayList<>();
        List<SegmentMetricDto> segmentMetrics = new ArrayList<>();
        for (BusinessMetric metric : factPackService.metrics(period)) {
            if ("core".equals(metric.getMetricScope())) {
                coreMetrics.add(toCoreMetric(metric));
            } else {
                segmentMetrics.add(toSegmentMetric(metric));
            }
        }
        response.setCoreMetrics(coreMetrics);
        response.setSegmentMetrics(segmentMetrics);
        response.setScenarioDefaults(toScenarioDefaults(factPackService.scenarios(period)));
        response.setCards(toCards(factPackService.cards()));
        List<InsightResult> results;
        if (metric_date != null && !metric_date.isEmpty()) {
            results = insightService.getInsightsByScenarioAndDate(scenario, metric_date);
        } else {
            results = insightService.getInsightsByScenario(scenario);
        }
        response.setResults(results == null ? Collections.<InsightResult>emptyList() : results);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> sourceOf(MetricSnapshot snapshot) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", snapshot.getSourceType());
        source.put("label", snapshot.getSourceLabel());
        source.put("file", snapshot.getSourceFile());
        source.put("imported_at", snapshot.getImportedAt());
        return source;
    }

    private CoreMetricDto toCoreMetric(BusinessMetric metric) {
        CoreMetricDto dto = new CoreMetricDto();
        dto.setMetricCode(metric.getMetricCode());
        dto.setMetricName(metric.getMetricName());
        dto.setValue(metric.getValue());
        dto.setDisplayValue(metric.getDisplayValue());
        dto.setUnit(metric.getUnit());
        dto.setYoy(metric.getYoy());
        dto.setDisplayYoy(metric.getDisplayYoy());
        dto.setMom(metric.getMom());
        dto.setBudget(metric.getBudget());
        dto.setBudgetGap(metric.getBudgetGap());
        dto.setDisplayBudgetGap(metric.getDisplayBudgetGap());
        dto.setRedline(metric.getRedline());
        dto.setYellowline(metric.getYellowline());
        dto.setDistanceToRedline(metric.getDistanceToRedline());
        dto.setDisplayDistanceToRedline(metric.getDisplayDistanceToRedline());
        dto.setDirection(metric.getDirection());
        dto.setModule(metric.getModule());
        return dto;
    }

    private SegmentMetricDto toSegmentMetric(BusinessMetric metric) {
        SegmentMetricDto dto = new SegmentMetricDto();
        dto.setMetricCode(metric.getMetricCode());
        dto.setMetricName(metric.getMetricName());
        dto.setDimension(metric.getDimensionKey());
        dto.setSegment(metric.getSegment());
        dto.setValue(metric.getValue());
        dto.setDisplayValue(metric.getDisplayValue());
        dto.setUnit(metric.getUnit());
        dto.setYoy(metric.getYoy());
        dto.setDisplayYoy(metric.getDisplayYoy());
        dto.setBudgetGap(metric.getBudgetGap());
        dto.setDisplayBudgetGap(metric.getDisplayBudgetGap());
        dto.setRedline(metric.getRedline());
        dto.setYellowline(metric.getYellowline());
        dto.setDistanceToRedline(metric.getDistanceToRedline());
        dto.setDisplayDistanceToRedline(metric.getDisplayDistanceToRedline());
        dto.setDirection(metric.getDirection());
        dto.setModule(metric.getModule());
        dto.setProductType(metric.getProductType());
        dto.setChannelType(metric.getChannelType());
        dto.setCustomerType(metric.getCustomerType());
        dto.setVintage(metric.getVintage());
        dto.setMob(metric.getMob());
        return dto;
    }

    private List<ScenarioDefaultDto> toScenarioDefaults(List<ScenarioBaseline> baselines) {
        List<ScenarioDefaultDto> result = new ArrayList<>();
        for (ScenarioBaseline baseline : baselines) {
            ScenarioDefaultDto dto = JsonUtils.fromJson(baseline.getInputsJson(), ScenarioDefaultDto.class);
            if (dto != null) {
                dto.setScenario(baseline.getScenario());
                dto.setScenarioLabel(baseline.getScenarioLabel());
                result.add(dto);
            }
        }
        return result;
    }

    private List<CardConfigDto> toCards(List<CardDefinition> definitions) {
        List<CardConfigDto> cards = new ArrayList<>();
        for (CardDefinition definition : definitions) {
            CardConfigDto dto = new CardConfigDto();
            dto.setCardId(definition.getCardId());
            dto.setCardName(definition.getTitle());
            dto.setTitle(definition.getTitle());
            dto.setModule(definition.getModule());
            dto.setDefaultPromptVersion(definition.getPromptVersion());
            dto.setEnabled(true);
            cards.add(dto);
        }
        return cards;
    }

    @GetMapping("/dashboard/mock")
    public ResponseEntity<Map<String, Object>> dashboardMock(@RequestParam(required = false) String scenario) {
        logger.info("Received dashboard mock data request for scenario: {}", scenario);

        String content = resourceUtils.readResource("data/dashboard_metrics_mock.json");
        if (content != null && !content.isEmpty()) {
            try {
                Map<String, Object> mockData = jsonUtils.fromJson(content, Map.class);
                if (mockData == null) {
                    throw new IllegalArgumentException("Invalid mock data JSON");
                }
                if (scenario != null && mockData.containsKey(scenario)) {
                    Map<String, Object> result = new HashMap<>();
                    result.put(scenario, mockData.get(scenario));
                    return ResponseEntity.ok(result);
                }
                return ResponseEntity.ok(mockData);
            } catch (Exception e) {
                logger.error("Failed to parse mock data", e);
            }
        }

        Map<String, Object> error = new HashMap<>();
        error.put("error", "Mock data not found");
        return ResponseEntity.status(404).body(error);
    }

    @GetMapping("/insights/{id}")
    public ResponseEntity<InsightResult> getInsightById(@PathVariable Long id) {
        InsightResult result = insightService.getInsightById(id);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/insights")
    public ResponseEntity<List<InsightResult>> getInsightsByScenario(@RequestParam String scenario) {
        List<InsightResult> results = insightService.getInsightsByScenario(scenario);
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/insights/{id}")
    public ResponseEntity<Void> deleteInsight(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }
}
