package com.lufax.dashboard.controller;

import com.lufax.dashboard.model.request.CardInsightRequest;
import com.lufax.dashboard.model.request.ManualOverrideRequest;
import com.lufax.dashboard.model.response.ManualOverrideResponse;
import com.lufax.dashboard.service.InsightService;
import com.lufax.dashboard.util.JsonUtils;
import com.lufax.dashboard.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/card")
public class CardController {

    private static final Logger logger = LoggerFactory.getLogger(CardController.class);

    @Autowired
    private InsightService insightService;

    @Autowired
    private ResourceUtils resourceUtils;

    @Autowired
    private JsonUtils jsonUtils;

    @PostMapping("/insight")
    public ResponseEntity<Map<String, Object>> cardInsight(@RequestBody CardInsightRequest request) {
        logger.info("Received card insight request for card_id: {}", request.getCardId());
        Map<String, Object> result = insightService.generateCardInsight(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> cardConfig(@RequestParam String card_id) {
        logger.info("Received card config request for card_id: {}", card_id);

        String content = resourceUtils.readResource("data/card_config.json");
        if (content != null && !content.isEmpty()) {
            try {
                Map<String, Object> configMap = jsonUtils.fromJson(content, Map.class);
                if (configMap.containsKey(card_id)) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("card_id", card_id);
                    result.put("config", configMap.get(card_id));
                    return ResponseEntity.ok(result);
                }
            } catch (Exception e) {
                logger.error("Failed to parse card config", e);
            }
        }

        Map<String, Object> error = new HashMap<>();
        error.put("error", "Card config not found");
        return ResponseEntity.status(404).body(error);
    }

    @GetMapping("/config/all")
    public ResponseEntity<Map<String, Object>> allCardConfigs() {
        logger.info("Received all card configs request");

        String content = resourceUtils.readResource("data/card_config.json");
        if (content != null && !content.isEmpty()) {
            try {
                Map<String, Object> configMap = jsonUtils.fromJson(content, Map.class);
                return ResponseEntity.ok(configMap);
            } catch (Exception e) {
                logger.error("Failed to parse card config", e);
            }
        }

        return ResponseEntity.ok(new HashMap<>());
    }

    @PostMapping("/override")
    public ResponseEntity<ManualOverrideResponse> manualOverride(@RequestBody ManualOverrideRequest request) {
        logger.info("Received manual override request for card_id: {}", request.getCardId());

        ManualOverrideResponse response = new ManualOverrideResponse();
        response.setSuccess(true);
        response.setMessage("Override recorded");
        response.setCardId(request.getCardId());
        response.setOverrideId(System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}