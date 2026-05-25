package com.lufax.dashboard.service;

import com.lufax.dashboard.util.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FactPackServiceTest {

    @Autowired
    private DemoSeedImportService importer;

    @Autowired
    private FactPackService service;

    @Before
    public void seed() {
        importer.importIfMissing();
    }

    @Test
    public void cardPackUsesDatabaseFactsWithoutImportedTrafficLight() {
        Map<String, Object> factPack = service.buildCardFactPack("2026-03-YTD", "risk_analysis");
        String json = JsonUtils.toJson(factPack);

        assertEquals("card", factPack.get("pack_type"));
        assertTrue(json.contains("credit_loss_rate"));
        assertTrue(json.contains("redline"));
        assertTrue(json.contains("演示数据，待替换"));
        assertFalse(json.contains("source_status"));
        assertFalse(json.contains("\"status\""));
    }

    @Test
    public void dashboardPackLoadsAllDatabaseFactsAndExplicitProvenance() {
        Map<String, Object> factPack = service.buildDashboardFactPack("2026-03-YTD");
        String json = JsonUtils.toJson(factPack);

        assertEquals("dashboard", factPack.get("pack_type"));
        assertTrue(json.contains("net_profit_management"));
        assertTrue(json.contains("segment_metrics"));
        assertTrue(json.contains("DEMO_SEED"));
    }

    @Test
    public void scenarioPackUsesStoredBearBaseBullInputs() {
        Map<String, Object> factPack = service.buildScenarioFactPack("2026-03-YTD", "bull", null);
        String json = JsonUtils.toJson(factPack);

        assertEquals("scenario", factPack.get("pack_type"));
        assertTrue(json.contains("\"scenario\":\"bull\""));
        assertTrue(json.contains("2200"));
        assertTrue(json.contains("credit_loss_rate"));
    }
}
