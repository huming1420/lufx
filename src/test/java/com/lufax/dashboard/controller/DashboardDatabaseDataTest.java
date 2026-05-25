package com.lufax.dashboard.controller;

import com.lufax.dashboard.model.response.DashboardDataResponse;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DashboardDatabaseDataTest {

    @Autowired
    private DemoSeedImportService importer;

    @Autowired
    private DashboardController controller;

    @Before
    public void seed() {
        importer.importIfMissing();
    }

    @Test
    public void dashboardDataReturnsImportedDatabaseFactsAndDemoProvenance() {
        ResponseEntity<DashboardDataResponse> response =
                controller.dashboardData("base", null);
        DashboardDataResponse body = response.getBody();

        assertNotNull(body);
        assertEquals("2026-03-YTD", body.getPeriod());
        assertEquals("演示数据，待替换", body.getDataSource().get("label"));
        assertFalse(body.getCoreMetrics().isEmpty());
        assertFalse(body.getSegmentMetrics().isEmpty());
        assertEquals(3, body.getScenarioDefaults().size());
    }
}
