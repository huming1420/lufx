package com.lufax.dashboard.service;

import com.lufax.dashboard.model.entity.InsightResult;
import com.lufax.dashboard.model.entity.MetricSnapshot;
import com.lufax.dashboard.model.request.CardInsightRequest;
import com.lufax.dashboard.model.request.DashboardInsightRequest;
import com.lufax.dashboard.model.request.ScenarioInsightRequest;
import com.lufax.dashboard.repository.BusinessFactMapper;
import com.lufax.dashboard.repository.InsightResultMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DatabaseBackedInsightServiceTest {

    @Autowired
    private DemoSeedImportService importer;

    @Autowired
    private InsightService service;

    @Autowired
    private BusinessFactMapper factMapper;

    @Autowired
    private InsightResultMapper resultMapper;

    @Before
    public void seed() {
        importer.importIfMissing();
    }

    @Test
    public void cardInsightReturnsAiLightLabelsAndPersistsDatabaseFactPack() {
        CardInsightRequest request = new CardInsightRequest();
        request.setPeriod("2026-03-YTD");
        request.setCardId("risk_analysis");

        Map<String, Object> result = service.generateCardInsight(request);

        assertEquals("red", result.get("traffic_light"));
        assertTrue(result.containsKey("analysis_labels"));
        assertEquals("演示数据，待替换",
                ((Map<?, ?>) result.get("data_source")).get("label"));

        MetricSnapshot snapshot = factMapper.selectActiveSnapshot("2026-03-YTD");
        InsightResult stored = resultMapper.selectByCacheKey(
                "2026-03-YTD", snapshot.getMetricVersion(), "v2", "card", "risk_analysis");
        assertNotNull(stored);
        assertTrue(stored.getRawFactPackJson().contains("credit_loss_rate"));
        assertTrue(!stored.getRawFactPackJson().contains("source_status"));
    }

    @Test
    public void defaultScenariosAreAiAnalysedFromStoredInputs() {
        for (String scenario : new String[]{"bear", "base", "bull"}) {
            ScenarioInsightRequest request = new ScenarioInsightRequest();
            request.setPeriod("2026-03-YTD");
            request.setScenario(scenario);

            Map<String, Object> result = service.generateScenarioInsight(request);

            assertEquals(scenario, result.get("scenario"));
            assertTrue(result.containsKey("traffic_light"));
            assertTrue(result.containsKey("analysis_labels"));
            assertTrue(result.containsKey("standard_explanation"));
        }
    }

    @Test
    public void changedScenarioInputsDoNotReuseDefaultCachedInsight() {
        ScenarioInsightRequest defaultRequest = new ScenarioInsightRequest();
        defaultRequest.setPeriod("2026-03-YTD");
        defaultRequest.setScenario("base");
        service.generateScenarioInsight(defaultRequest);

        ScenarioInsightRequest adjustedRequest = new ScenarioInsightRequest();
        adjustedRequest.setPeriod("2026-03-YTD");
        adjustedRequest.setScenario("base");
        Map<String, Object> adjustedInputs = new LinkedHashMap<>();
        adjustedInputs.put("credit_loss_rate", 9.9);
        adjustedRequest.setInputs(adjustedInputs);

        Map<String, Object> adjusted = service.generateScenarioInsight(adjustedRequest);

        assertEquals(9.9, ((Map<?, ?>) adjusted.get("inputs")).get("credit_loss_rate"));
    }

    @Test
    public void dashboardInsightReturnsAiOverallTrafficLight() {
        DashboardInsightRequest request = new DashboardInsightRequest();
        request.setPeriod("2026-03-YTD");

        Map<String, Object> result = service.generateDashboardInsight(request);

        assertEquals("red", result.get("overall_traffic_light"));
        assertTrue(result.containsKey("analysis_labels"));
    }
}
