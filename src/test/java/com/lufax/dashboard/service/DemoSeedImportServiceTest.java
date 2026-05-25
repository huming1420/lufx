package com.lufax.dashboard.service;

import com.lufax.dashboard.model.entity.MetricSnapshot;
import com.lufax.dashboard.repository.BusinessFactMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DemoSeedImportServiceTest {

    @Autowired
    private DemoSeedImportService service;

    @Autowired
    private BusinessFactMapper mapper;

    @Test
    public void importsDemoJsonWithProvenanceMetricsCardsAndScenarios() {
        service.importIfMissing();

        MetricSnapshot snapshot = mapper.selectActiveSnapshot("2026-03-YTD");
        assertEquals("mock_v2", snapshot.getMetricVersion());
        assertEquals("DEMO_SEED", snapshot.getSourceType());
        assertEquals("演示数据，待替换", snapshot.getSourceLabel());
        assertFalse(mapper.selectMetrics(snapshot.getPeriod(), snapshot.getMetricVersion()).isEmpty());
        assertFalse(mapper.selectCards().isEmpty());
        assertEquals(3, mapper.selectScenarioBaselines(snapshot.getPeriod(), snapshot.getMetricVersion()).size());
    }

    @Test
    public void repeatedImportIsIdempotent() {
        service.importIfMissing();
        service.importIfMissing();

        assertEquals(1, mapper.countSnapshots("2026-03-YTD", "mock_v2"));
    }
}
