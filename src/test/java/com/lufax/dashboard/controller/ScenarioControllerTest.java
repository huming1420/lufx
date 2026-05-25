package com.lufax.dashboard.controller;

import com.lufax.dashboard.TestDataFactory;
import com.lufax.dashboard.model.request.ScenarioInsightRequest;
import com.lufax.dashboard.service.InsightService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ScenarioController}.
 * Covers all 3 endpoints: POST /insight, GET /list, GET /{scenario}
 */
public class ScenarioControllerTest {

    @Mock
    private InsightService insightService;

    @InjectMocks
    private ScenarioController scenarioController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    // ========== POST /api/scenario/insight ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testScenarioInsight_Success() {
        // Arrange
        ScenarioInsightRequest request = TestDataFactory.createScenarioRequest("base");
        Map<String, Object> expectedResult = new LinkedHashMap<>();
        expectedResult.put("scenario", "base");
        expectedResult.put("score", 72.5);
        when(insightService.generateScenarioInsight(any(ScenarioInsightRequest.class))).thenReturn(expectedResult);

        // Act
        ResponseEntity<Map<String, Object>> response = scenarioController.scenarioInsight(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("base", response.getBody().get("scenario"));
        assertEquals(72.5, response.getBody().get("score"));
        verify(insightService).generateScenarioInsight(request);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testScenarioInsight_DifferentScenario() {
        // Arrange
        ScenarioInsightRequest request = TestDataFactory.createScenarioRequest("bear");
        Map<String, Object> expectedResult = new LinkedHashMap<>();
        expectedResult.put("scenario", "bear");
        expectedResult.put("summary", "Bearish scenario insight");
        when(insightService.generateScenarioInsight(any(ScenarioInsightRequest.class))).thenReturn(expectedResult);

        // Act
        ResponseEntity<Map<String, Object>> response = scenarioController.scenarioInsight(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("bear", response.getBody().get("scenario"));
        verify(insightService).generateScenarioInsight(argThat(req -> "bear".equals(req.getScenario())));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testScenarioInsight_PassthroughBehavior() {
        // Arrange - verify controller is a thin wrapper
        ScenarioInsightRequest request = TestDataFactory.createScenarioRequest();
        Map<String, Object> serviceResult = new LinkedHashMap<>();
        serviceResult.put("scenario", "base");
        serviceResult.put("insight_text", "Detailed analysis here");
        serviceResult.put("recommendations", Arrays.asList("Action 1", "Action 2"));
        serviceResult.put("confidence", 0.85);
        when(insightService.generateScenarioInsight(any(ScenarioInsightRequest.class))).thenReturn(serviceResult);

        // Act
        ResponseEntity<Map<String, Object>> response = scenarioController.scenarioInsight(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(4, response.getBody().size());
        assertEquals("Detailed analysis here", response.getBody().get("insight_text"));
    }

    // ========== GET /api/scenario/list ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testListScenarios_ReturnsAllScenarios() {
        // Act
        ResponseEntity<Map<String, Object>> response = scenarioController.listScenarios();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("scenarios"));

        String[] scenarios = (String[]) response.getBody().get("scenarios");
        assertNotNull(scenarios);
        assertEquals(5, scenarios.length);
        // Verify expected scenarios are present
        assertTrue(Arrays.asList(scenarios).contains("dashboard_overview"));
        assertTrue(Arrays.asList(scenarios).contains("channel_analysis"));
        assertTrue(Arrays.asList(scenarios).contains("product_performance"));
        assertTrue(Arrays.asList(scenarios).contains("user_behavior"));
        assertTrue(Arrays.asList(scenarios).contains("risk_management"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListScenarios_FirstScenarioIsDashboardOverview() {
        // Act
        ResponseEntity<Map<String, Object>> response = scenarioController.listScenarios();

        // Assert
        String[] scenarios = (String[]) response.getBody().get("scenarios");
        assertEquals("dashboard_overview", scenarios[0]);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListScenarios_ContainsRiskManagement() {
        // Act
        ResponseEntity<Map<String, Object>> response = scenarioController.listScenarios();

        // Assert
        String[] scenarios = (String[]) response.getBody().get("scenarios");
        assertEquals("risk_management", scenarios[scenarios.length - 1]);
    }

    // ========== GET /api/scenario/{scenario} ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testGetScenarioInfo_ValidScenario() {
        // Arrange
        String scenarioName = "channel_analysis";

        // Act
        ResponseEntity<Map<String, Object>> response = scenarioController.getScenarioInfo(scenarioName);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(scenarioName, response.getBody().get("scenario"));
        assertEquals("Scenario " + scenarioName, response.getBody().get("description"));

        String[] supportedMetrics = (String[]) response.getBody().get("supported_metrics");
        assertNotNull(supportedMetrics);
        assertEquals(4, supportedMetrics.length);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetScenarioInfo_SupportedMetricsContent() {
        // Act
        ResponseEntity<Map<String, Object>> response = scenarioController.getScenarioInfo("test");

        // Assert - verify the supported metrics array content
        String[] supportedMetrics = (String[]) response.getBody().get("supported_metrics");
        assertTrue(Arrays.asList(supportedMetrics).contains("score"));
        assertTrue(Arrays.asList(supportedMetrics).contains("offset_factor"));
        assertTrue(Arrays.asList(supportedMetrics).contains("total_amount"));
        assertTrue(Arrays.asList(supportedMetrics).contains("count"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetScenarioInfo_DescriptionIncludesScenarioName() {
        // Arrange
        String customScenario = "custom_scenario_v2";

        // Act
        ResponseEntity<Map<String, Object>> response = scenarioController.getScenarioInfo(customScenario);

        // Assert
        assertEquals("Scenario " + customScenario, response.getBody().get("description"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetScenarioInfo_AlwaysReturns200() {
        // The endpoint always returns 200 regardless of whether scenario exists
        for (String name : new String[]{"base", "bear", "bull", "unknown_scenario", ""}) {
            ResponseEntity<Map<String, Object>> response = scenarioController.getScenarioInfo(name);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(name, response.getBody().get("scenario"));
        }
    }
}
