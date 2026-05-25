package com.lufax.dashboard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lufax.dashboard.model.entity.BusinessMetric;
import com.lufax.dashboard.model.entity.CardDefinition;
import com.lufax.dashboard.model.entity.MetricSnapshot;
import com.lufax.dashboard.model.entity.ScenarioBaseline;
import com.lufax.dashboard.repository.BusinessFactMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;

@Service
public class DemoSeedImportService {
    private static final Logger logger = LoggerFactory.getLogger(DemoSeedImportService.class);
    private static final String METRICS_FILE = "data/dashboard_metrics_mock.json";
    private static final String CARD_FILE = "data/card_config.json";
    private static final String SOURCE_LABEL = "演示数据，待替换";

    @Autowired
    private BusinessFactMapper mapper;

    @Value("${app.demo-seed.enabled:true}")
    private boolean enabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initializeDemoSeed() {
        if (enabled) {
            importIfMissing();
        }
    }

    @Transactional
    public void importIfMissing() {
        JsonNode root = readTree(METRICS_FILE);
        String period = root.path("period").asText();
        String version = root.path("metric_version").asText();
        if (mapper.countSnapshots(period, version) > 0) {
            return;
        }

        MetricSnapshot snapshot = new MetricSnapshot();
        snapshot.setPeriod(period);
        snapshot.setMetricVersion(version);
        snapshot.setSourceType("DEMO_SEED");
        snapshot.setSourceLabel(SOURCE_LABEL);
        snapshot.setSourceFile(METRICS_FILE);
        snapshot.setImportedAt(new Timestamp(System.currentTimeMillis()));
        mapper.insertSnapshot(snapshot);

        int index = 0;
        for (JsonNode node : root.path("core_metrics")) {
            mapper.insertMetric(toMetric(node, period, version, "core", "", index++));
        }
        for (JsonNode node : root.path("segment_metrics")) {
            String dimension = node.path("dimension").asText("");
            mapper.insertMetric(toMetric(node, period, version, "segment", dimension, index++));
        }
        for (JsonNode scenario : root.path("scenario_defaults")) {
            ScenarioBaseline baseline = new ScenarioBaseline();
            baseline.setPeriod(period);
            baseline.setMetricVersion(version);
            baseline.setScenario(scenario.path("scenario").asText());
            baseline.setScenarioLabel(scenario.path("scenario_label").asText());
            baseline.setInputsJson(scenario.toString());
            mapper.insertScenarioBaseline(baseline);
        }
        for (JsonNode cardNode : readTree(CARD_FILE)) {
            CardDefinition card = new CardDefinition();
            card.setCardId(cardNode.path("card_id").asText());
            card.setTitle(cardNode.path("title").asText());
            card.setModule(cardNode.path("module").asText());
            card.setPromptVersion(cardNode.path("default_prompt_version").asText("v2"));
            mapper.insertCard(card);
            for (JsonNode metricCode : cardNode.path("metric_codes")) {
                mapper.insertCardMetric(card.getCardId(), metricCode.asText());
            }
        }
        logger.info("Initialized demo business fact snapshot {} / {} from {}", period, version, METRICS_FILE);
    }

    private BusinessMetric toMetric(JsonNode node, String period, String version,
                                    String scope, String dimension, int index) {
        BusinessMetric metric = new BusinessMetric();
        metric.setPeriod(period);
        metric.setMetricVersion(version);
        metric.setMetricScope(scope);
        metric.setMetricCode(text(node, "metric_code"));
        metric.setMetricName(text(node, "metric_name"));
        metric.setModule(text(node, "module"));
        metric.setDimensionKey("core".equals(scope) ? "" :
                dimension + ":" + node.path("segment").asText("") + ":" + index);
        metric.setSegment(text(node, "segment"));
        metric.setValue(number(node, "value"));
        metric.setDisplayValue(text(node, "display_value"));
        metric.setUnit(text(node, "unit"));
        metric.setYoy(number(node, "yoy"));
        metric.setDisplayYoy(text(node, "display_yoy"));
        metric.setMom(number(node, "mom"));
        metric.setBudget(number(node, "budget"));
        metric.setBudgetGap(number(node, "budget_gap"));
        metric.setDisplayBudgetGap(text(node, "display_budget_gap"));
        metric.setRedline(number(node, "redline"));
        metric.setYellowline(number(node, "yellowline"));
        metric.setDistanceToRedline(number(node, "distance_to_redline"));
        metric.setDisplayDistanceToRedline(text(node, "display_distance_to_redline"));
        metric.setDirection(text(node, "direction"));
        metric.setSourceStatus(text(node, "status"));
        metric.setProductType(text(node, "product_type"));
        metric.setChannelType(text(node, "channel_type"));
        metric.setCustomerType(text(node, "customer_type"));
        metric.setVintage(text(node, "vintage"));
        metric.setMob(text(node, "mob"));
        return metric;
    }

    private JsonNode readTree(String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readTree(input);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to import demo source " + path, e);
        }
    }

    private String text(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private Double number(JsonNode node, String field) {
        return node.has(field) && node.get(field).isNumber() ? node.get(field).asDouble() : null;
    }
}
