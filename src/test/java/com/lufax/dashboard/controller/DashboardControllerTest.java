package com.lufax.dashboard.controller;

import com.lufax.dashboard.model.entity.InsightResult;
import com.lufax.dashboard.model.entity.MetricSnapshot;
import com.lufax.dashboard.model.request.DashboardInsightRequest;
import com.lufax.dashboard.model.response.DashboardDataResponse;
import com.lufax.dashboard.model.response.HealthResponse;
import com.lufax.dashboard.service.InsightService;
import com.lufax.dashboard.service.FactPackService;
import com.lufax.dashboard.util.JsonUtils;
import com.lufax.dashboard.util.ResourceUtils;
import com.lufax.dashboard.TestDataFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DashboardController 单元测试 (Mock)
 * 覆盖: /health, /dashboard/insight, /dashboard/data, /dashboard/mock,
 *       /insights/{id}, /insights, DELETE /insights/{id}
 */
public class DashboardControllerTest {

    @Mock
    private InsightService insightService;

    @Mock
    private FactPackService factPackService;

    @Mock
    private ResourceUtils resourceUtils;

    @Mock
    private JsonUtils jsonUtils;

    @InjectMocks
    private DashboardController controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        MetricSnapshot snapshot = new MetricSnapshot();
        snapshot.setPeriod("2026-03-YTD");
        snapshot.setMetricVersion("mock_v2");
        snapshot.setSourceType("DEMO_SEED");
        snapshot.setSourceLabel("演示数据，待替换");
        when(factPackService.activeSnapshot(nullable(String.class))).thenReturn(snapshot);
        when(factPackService.metrics(nullable(String.class))).thenReturn(Collections.emptyList());
        when(factPackService.scenarios(nullable(String.class))).thenReturn(Collections.emptyList());
        when(factPackService.cards()).thenReturn(Collections.emptyList());
    }

    // ========== GET /health ==========

    @Test
    public void testHealth_ReturnsUpStatus() {
        ResponseEntity<HealthResponse> response = controller.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().getStatus());
        assertEquals("dashboard-backend", response.getBody().getService());
    }

    // ========== POST /dashboard/insight ==========

    @Test
    public void testDashboardInsight_ReturnsOkWithResult() {
        DashboardInsightRequest request = new DashboardInsightRequest();
        request.setScenario("growth");
        request.setMetricDate("2025-06-15");
        request.setReportType("monthly");

        Map<String, Object> serviceResult = new LinkedHashMap<>();
        serviceResult.put("scenario", "growth");
        serviceResult.put("executive_summary", "Q2 performance overview");
        when(insightService.generateDashboardInsight(any())).thenReturn(serviceResult);

        ResponseEntity<Map<String, Object>> response = controller.dashboardInsight(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("growth", response.getBody().get("scenario"));
        verify(insightService).generateDashboardInsight(request);
    }

    @Test
    public void testDashboardInsight_PassesRequestToService() {
        DashboardInsightRequest request = TestDataFactory.createDashboardRequest();

        controller.dashboardInsight(request);

        verify(insightService).generateDashboardInsight(request);
    }

    // ========== GET /dashboard/data (带 metric_date) ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testDashboardData_WithMetricDate_CallsByScenarioAndDate() {
        String scenario = "growth";
        String metricDate = "2025-06-15";

        List<InsightResult> expected = Collections.singletonList(
                TestDataFactory.createInsightResult(1L));
        when(insightService.getInsightsByScenarioAndDate(scenario, metricDate))
                .thenReturn(expected);

        ResponseEntity<DashboardDataResponse> response = controller.dashboardData(scenario, metricDate);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(scenario, response.getBody().getScenario());
        assertEquals(metricDate, response.getBody().getMetricDate());
        assertEquals(1, response.getBody().getResults().size());

        verify(insightService).getInsightsByScenarioAndDate(scenario, metricDate);
        verify(insightService, never()).getInsightsByScenario(anyString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDashboardData_WithoutMetricDate_CallsByScenarioOnly() {
        String scenario = "growth";

        List<InsightResult> expected = Arrays.asList(
                TestDataFactory.createInsightResult(1L),
                TestDataFactory.createInsightResult(2L));
        when(insightService.getInsightsByScenario(scenario)).thenReturn(expected);

        ResponseEntity<DashboardDataResponse> response = controller.dashboardData(scenario, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().getResults().size());
        assertNull(response.getBody().getMetricDate()); // 未传入时为 null

        verify(insightService).getInsightsByScenario(scenario);
        verify(insightService, never()).getInsightsByScenarioAndDate(anyString(), anyString());
    }

    // ========== GET /dashboard/mock ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testDashboardMock_MockFileExists_ReturnsData() {
        Map<String, Object> mockData = new LinkedHashMap<>();
        mockData.put("growth", "mock growth data");
        mockData.put("decline", "mock decline data");

        when(resourceUtils.readResource("data/dashboard_metrics_mock.json"))
                .thenReturn("{\"growth\":\"mock growth data\",\"decline\":\"mock decline data\"}");
        ResponseEntity<Map<String, Object>> response = controller.dashboardMock(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("growth"));
        assertTrue(response.getBody().containsKey("decline"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDashboardMock_WithScenarioFilter() {
        Map<String, Object> mockData = new LinkedHashMap<>();
        mockData.put("growth", "mock data 1");
        mockData.put("decline", "mock data 2");

        when(resourceUtils.readResource("data/dashboard_metrics_mock.json"))
                .thenReturn("{\"growth\":\"mock data 1\"}");
        ResponseEntity<Map<String, Object>> response = controller.dashboardMock("growth");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertTrue(response.getBody().containsKey("growth"));
    }

    @Test
    public void testDashboardMock_FileNotFound_Returns404() {
        when(resourceUtils.readResource("data/dashboard_metrics_mock.json")).thenReturn("");

        ResponseEntity<Map<String, Object>> response = controller.dashboardMock(null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().containsKey("error"));
        assertEquals("Mock data not found", response.getBody().get("error"));
    }

    @Test
    public void testDashboardMock_InvalidJson_Returns404() {
        when(resourceUtils.readResource("data/dashboard_metrics_mock.json")).thenReturn("invalid {json");
        ResponseEntity<Map<String, Object>> response = controller.dashboardMock(null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ========== GET /insights/{id} ==========

    @Test
    public void testGetInsightById_Found_Returns200() {
        InsightResult result = TestDataFactory.createInsightResult(42L);
        when(insightService.getInsightById(42L)).thenReturn(result);

        ResponseEntity<InsightResult> response = controller.getInsightById(42L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Long.valueOf(42L), response.getBody().getId());
    }

    @Test
    public void testGetInsightById_NotFound_Returns404() {
        when(insightService.getInsightById(999L)).thenReturn(null);

        ResponseEntity<InsightResult> response = controller.getInsightById(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ========== GET /insights?scenario=xxx ==========

    @Test
    public void testGetInsightsByScenario_ReturnsList() {
        List<InsightResult> expected = Arrays.asList(
                TestDataFactory.createInsightResult(1L),
                TestDataFactory.createInsightResult(2L)
        );
        when(insightService.getInsightsByScenario("growth")).thenReturn(expected);

        ResponseEntity<List<InsightResult>> response = controller.getInsightsByScenario("growth");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    public void testGetInsightsByScenario_EmptyList_Returns200() {
        when(insightService.getInsightsByScenario("empty_scenario"))
                .thenReturn(Collections.emptyList());

        ResponseEntity<List<InsightResult>> response = controller.getInsightsByScenario("empty_scenario");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // ========== DELETE /insights/{id} ==========

    @Test
    public void testDeleteInsight_Returns204NoContent() {
        ResponseEntity<Void> response = controller.deleteInsight(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }
}
