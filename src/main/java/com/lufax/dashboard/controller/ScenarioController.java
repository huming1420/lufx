package com.lufax.dashboard.controller;

import com.lufax.dashboard.model.request.ScenarioInsightRequest;
import com.lufax.dashboard.service.InsightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scenario")
public class ScenarioController {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioController.class);

    @Autowired
    private InsightService insightService;

    @PostMapping("/insight")
    public ResponseEntity<Map<String, Object>> scenarioInsight(@RequestBody ScenarioInsightRequest request) {
        logger.info("Received scenario insight request for scenario: {}", request.getScenario());
        Map<String, Object> result = insightService.generateScenarioInsight(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listScenarios() {
        logger.info("Received list scenarios request");

        Map<String, Object> result = new HashMap<>();
        result.put("scenarios", new String[]{
            "dashboard_overview",
            "channel_analysis",
            "product_performance",
            "user_behavior",
            "risk_management"
        });

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{scenario}")
    public ResponseEntity<Map<String, Object>> getScenarioInfo(@PathVariable String scenario) {
        logger.info("Received scenario info request for: {}", scenario);

        Map<String, Object> info = new HashMap<>();
        info.put("scenario", scenario);
        info.put("description", "Scenario " + scenario);
        info.put("supported_metrics", new String[]{"score", "offset_factor", "total_amount", "count"});

        return ResponseEntity.ok(info);
    }
}