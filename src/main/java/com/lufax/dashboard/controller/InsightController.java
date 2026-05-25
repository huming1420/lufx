package com.lufax.dashboard.controller;

import com.lufax.dashboard.model.entity.CardDefinition;
import com.lufax.dashboard.model.entity.MetricSnapshot;
import com.lufax.dashboard.model.request.CardInsightRequest;
import com.lufax.dashboard.model.request.DashboardInsightRequest;
import com.lufax.dashboard.model.request.ScenarioInsightRequest;
import com.lufax.dashboard.service.FactPackService;
import com.lufax.dashboard.service.InsightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/insights")
public class InsightController {

    @Autowired
    private InsightService insightService;

    @Autowired
    private FactPackService factPackService;

    private final Map<String, Map<String, Object>> jobs = new ConcurrentHashMap<>();

    @GetMapping("/page-state")
    public ResponseEntity<Map<String, Object>> pageState(
            @RequestParam(defaultValue = "2026-03-YTD") String period) {
        Map<String, Object> response = header(period);
        List<Map<String, Object>> cards = new ArrayList<>();
        for (CardDefinition card : factPackService.cards()) {
            CardInsightRequest request = new CardInsightRequest();
            request.setPeriod(period);
            request.setCardId(card.getCardId());
            Map<String, Object> insight = insightService.generateCardInsight(request);
            insight.put("title", card.getTitle());
            insight.put("module", card.getModule());
            cards.add(insight);
        }
        response.put("cards", cards);
        response.put("dashboard", generateDashboard(period));
        response.put("scenarios", generateScenarios(period));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(
            @RequestParam(defaultValue = "2026-03-YTD") String period) {
        return ResponseEntity.ok(generateDashboard(period));
    }

    @PostMapping("/scenario")
    public ResponseEntity<Map<String, Object>> scenario(@RequestBody ScenarioInsightRequest request) {
        return ResponseEntity.ok(insightService.generateScenarioInsight(request));
    }

    @PostMapping("/jobs")
    public ResponseEntity<Map<String, Object>> createJob(
            @RequestBody(required = false) Map<String, Object> request) {
        String period = request != null && request.get("period") != null
                ? String.valueOf(request.get("period")) : "2026-03-YTD";
        String jobId = UUID.randomUUID().toString();
        Map<String, Object> state = header(period);
        state.put("job_id", jobId);
        state.put("status", "running");
        jobs.put(jobId, state);
        try {
            pageState(period);
            state.put("status", "ready");
        } catch (RuntimeException e) {
            state.put("status", "failed");
            state.put("error", e.getMessage());
        }
        return ResponseEntity.ok(state);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, Object>> job(@PathVariable String jobId) {
        Map<String, Object> state = jobs.get(jobId);
        return state == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(state);
    }

    private Map<String, Object> generateDashboard(String period) {
        DashboardInsightRequest request = new DashboardInsightRequest();
        request.setPeriod(period);
        return insightService.generateDashboardInsight(request);
    }

    private List<Map<String, Object>> generateScenarios(String period) {
        List<Map<String, Object>> scenarios = new ArrayList<>();
        for (String name : Arrays.asList("bear", "base", "bull")) {
            ScenarioInsightRequest request = new ScenarioInsightRequest();
            request.setPeriod(period);
            request.setScenario(name);
            scenarios.add(insightService.generateScenarioInsight(request));
        }
        return scenarios;
    }

    private Map<String, Object> header(String period) {
        MetricSnapshot snapshot = factPackService.activeSnapshot(period);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("period", snapshot.getPeriod());
        response.put("metric_version", snapshot.getMetricVersion());
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("type", snapshot.getSourceType());
        source.put("label", snapshot.getSourceLabel());
        source.put("file", snapshot.getSourceFile());
        response.put("data_source", source);
        return response;
    }
}
