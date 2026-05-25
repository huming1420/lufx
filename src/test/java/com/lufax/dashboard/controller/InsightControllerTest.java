package com.lufax.dashboard.controller;

import com.lufax.dashboard.model.request.ScenarioInsightRequest;
import com.lufax.dashboard.service.DemoSeedImportService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InsightControllerTest {

    @Autowired
    private DemoSeedImportService importer;

    @Autowired
    private InsightController controller;

    @Before
    public void seed() {
        importer.importIfMissing();
    }

    @Test
    public void pageStateProvidesAiCardsDashboardScenariosAndDemoLabel() {
        ResponseEntity<Map<String, Object>> response = controller.pageState("2026-03-YTD");
        Map<String, Object> body = response.getBody();

        assertEquals("演示数据，待替换",
                ((Map<?, ?>) body.get("data_source")).get("label"));
        assertEquals(6, ((List<?>) body.get("cards")).size());
        assertEquals(3, ((List<?>) body.get("scenarios")).size());
        assertTrue(((Map<?, ?>) body.get("dashboard")).containsKey("overall_traffic_light"));
    }

    @Test
    public void scenarioEndpointProvidesAiDefaultResult() {
        ScenarioInsightRequest request = new ScenarioInsightRequest();
        request.setPeriod("2026-03-YTD");
        request.setScenario("base");

        Map<String, Object> result = controller.scenario(request).getBody();

        assertEquals("base", result.get("scenario"));
        assertTrue(result.containsKey("traffic_light"));
        assertTrue(result.containsKey("standard_explanation"));
    }
}
