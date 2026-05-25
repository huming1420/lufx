package com.lufax.dashboard.repository;

import com.lufax.dashboard.model.entity.BusinessMetric;
import com.lufax.dashboard.model.entity.CardDefinition;
import com.lufax.dashboard.model.entity.MetricSnapshot;
import com.lufax.dashboard.model.entity.ScenarioBaseline;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BusinessFactMapperTest {

    @Autowired
    private BusinessFactMapper mapper;

    @Test
    public void selectActiveSnapshotReturnsNewestImport() {
        mapper.insertSnapshot(snapshot("v1", "2026-05-20 10:00:00"));
        mapper.insertSnapshot(snapshot("v2", "2026-05-21 10:00:00"));

        MetricSnapshot active = mapper.selectActiveSnapshot("2026-03-YTD");

        assertEquals("v2", active.getMetricVersion());
        assertEquals("DEMO_SEED", active.getSourceType());
    }

    @Test
    public void selectCardMetricsReturnsBoundFactsWithTraceStatus() {
        mapper.insertSnapshot(snapshot("v1", "2026-05-20 10:00:00"));
        CardDefinition card = new CardDefinition();
        card.setCardId("risk_analysis");
        card.setTitle("Risk");
        card.setModule("risk");
        mapper.insertCard(card);
        mapper.insertCardMetric("risk_analysis", "credit_loss_rate");

        BusinessMetric metric = new BusinessMetric();
        metric.setPeriod("2026-03-YTD");
        metric.setMetricVersion("v1");
        metric.setMetricScope("core");
        metric.setMetricCode("credit_loss_rate");
        metric.setDimensionKey("");
        metric.setValue(7.2);
        metric.setRedline(8.0);
        metric.setSourceStatus("red");
        mapper.insertMetric(metric);

        List<BusinessMetric> metrics =
                mapper.selectMetricsForCard("2026-03-YTD", "v1", "risk_analysis");

        assertEquals(1, metrics.size());
        assertEquals("credit_loss_rate", metrics.get(0).getMetricCode());
        assertEquals("red", metrics.get(0).getSourceStatus());
        assertEquals(Double.valueOf(8.0), metrics.get(0).getRedline());
    }

    @Test
    public void selectScenarioBaselinesReturnsBearBaseBull() {
        mapper.insertScenarioBaseline(scenario("bear"));
        mapper.insertScenarioBaseline(scenario("base"));
        mapper.insertScenarioBaseline(scenario("bull"));

        List<ScenarioBaseline> scenarios =
                mapper.selectScenarioBaselines("2026-03-YTD", "v1");

        assertEquals(3, scenarios.size());
        assertEquals("bear", scenarios.get(0).getScenario());
        assertEquals("bull", scenarios.get(2).getScenario());
    }

    private MetricSnapshot snapshot(String version, String importedAt) {
        MetricSnapshot snapshot = new MetricSnapshot();
        snapshot.setPeriod("2026-03-YTD");
        snapshot.setMetricVersion(version);
        snapshot.setSourceType("DEMO_SEED");
        snapshot.setSourceLabel("演示数据，待替换");
        snapshot.setImportedAt(Timestamp.valueOf(importedAt));
        return snapshot;
    }

    private ScenarioBaseline scenario(String scenario) {
        ScenarioBaseline baseline = new ScenarioBaseline();
        baseline.setPeriod("2026-03-YTD");
        baseline.setMetricVersion("v1");
        baseline.setScenario(scenario);
        baseline.setScenarioLabel(scenario);
        baseline.setInputsJson("{}");
        return baseline;
    }
}
