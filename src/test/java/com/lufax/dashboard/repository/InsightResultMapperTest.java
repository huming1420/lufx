package com.lufax.dashboard.repository;

import com.lufax.dashboard.model.entity.InsightResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for {@link InsightResultMapper}.
 * Uses H2 in-memory database with PostgreSQL compatibility mode.
 * All tests run within a transaction that rolls back after each test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InsightResultMapperTest {

    @Autowired
    private InsightResultMapper insightResultMapper;

    // ========== Helper: create a valid InsightResult entity ==========
    private InsightResult createTestResult(String period, String cardId) {
        InsightResult result = new InsightResult();
        result.setPeriod(period);
        result.setMetricVersion("v1.2.0");
        result.setPromptVersion("p3.1.0");
        result.setInsightType("card");
        result.setCardId(cardId);
        result.setStatus("pending");
        result.setValidated(false);
        return result;
    }

    // ========== insertOrUpdate / upsert ==========

    @Test
    public void testInsertOrUpdate_NewRecord() {
        // Arrange
        InsightResult record = createTestResult("2026-03-YTD", "profit_analysis");

        // Act
        int rows = insightResultMapper.insertOrUpdate(record);

        // Assert
        assertEquals(1, rows);
        assertNotNull(record.getId());
        assertTrue(record.getId() > 0);
    }

    @Test
    public void testInsertOrUpdate_SetsTimestamps() {
        // Arrange
        InsightResult record = createTestResult("2026-Q1", "revenue_trend");

        // Act
        insightResultMapper.insertOrUpdate(record);

        // Assert - timestamps should be set by DB (now())
        assertNotNull(record.getCreatedAt());
        assertNotNull(record.getUpdatedAt());
    }

    @Test
    public void testInsertOrUpdate_PreservesAllFields() {
        // Arrange
        InsightResult record = createTestResult("2026-03-YTD", "card1");
        record.setStatus("ready");
        record.setRawLlmOutput("raw output here");
        record.setRawLlmOutputJson("{\"key\":\"value\"}");
        record.setResultJson("{\"result\":\"ok\"}");
        record.setErrorMessage(null);  // no error
        record.setValidated(true);

        // Act
        insightResultMapper.insertOrUpdate(record);

        // Assert - retrieve and verify
        InsightResult found = insightResultMapper.selectById(record.getId());
        assertNotNull(found);
        assertEquals("ready", found.getStatus());
        assertEquals("raw output here", found.getRawLlmOutput());
        assertTrue(found.getValidated());
    }

    // ========== selectByCacheKey ==========

    @Test
    public void testSelectByCacheKey_Found() {
        // Arrange
        InsightResult record = createTestResult("2026-03-YTD", "card_a");
        record.setMetricVersion("mv1");
        record.setPromptVersion("pv1");
        record.setInsightType("scenario");
        insightResultMapper.insertOrUpdate(record);

        // Act
        InsightResult found = insightResultMapper.selectByCacheKey(
                "2026-03-YTD", "mv1", "pv1", "scenario", "card_a");

        // Assert
        assertNotNull(found);
        assertEquals(record.getId(), found.getId());
        assertEquals("2026-03-YTD", found.getPeriod());
        assertEquals("card_a", found.getCardId());
        assertEquals("pending", found.getStatus());
    }

    @Test
    public void testSelectByCacheKey_NotFound() {
        // Act
        InsightResult found = insightResultMapper.selectByCacheKey(
                "nonexistent", "mv99", "pv99", "unknown", "card_z");

        // Assert
        assertNull(found);
    }

    @Test
    public void testSelectByCacheKey_CaseSensitive() {
        // Arrange
        InsightResult record = createTestResult("2026-03-YTD", "CardA");
        insightResultMapper.insertOrUpdate(record);

        // Act - search with different case
        InsightResult found = insightResultMapper.selectByCacheKey(
                "2026-03-YTD", "mv1", "pv1", "card", "carda");

        // Assert - should not find (case-sensitive match)
        assertNull(found);
    }

    @Test
    public void testSelectByCacheKey_AllFiveParamsMustMatch() {
        // Arrange - insert two records with same period but different cards
        InsightResult r1 = createTestResult("2026-Q1", "card_x");
        r1.setMetricVersion("m1");
        r1.setPromptVersion("p1");
        r1.setInsightType("dashboard");
        insightResultMapper.insertOrUpdate(r1);

        InsightResult r2 = createTestResult("2026-Q1", "card_y");
        r2.setMetricVersion("m1");
        r2.setPromptVersion("p1");
        r2.setInsightType("dashboard");
        insightResultMapper.insertOrUpdate(r2);

        // Act
        InsightResult found1 = insightResultMapper.selectByCacheKey("2026-Q1", "m1", "p1", "dashboard", "card_x");
        InsightResult found2 = insightResultMapper.selectByCacheKey("2026-Q1", "m1", "p1", "dashboard", "card_y");

        // Assert
        assertNotNull(found1);
        assertNotNull(found2);
        assertNotEquals(found1.getId(), found2.getId());
    }

    // ========== selectById ==========

    @Test
    public void testSelectById_Found() {
        // Arrange
        InsightResult record = createTestResult("2026-03-YTD", "test_card");
        insightResultMapper.insertOrUpdate(record);
        Long generatedId = record.getId();

        // Act
        InsightResult found = insightResultMapper.selectById(generatedId);

        // Assert
        assertNotNull(found);
        assertEquals(generatedId, found.getId());
        assertEquals("test_card", found.getCardId());
        assertEquals("pending", found.getStatus());
        assertEquals("v1.2.0", found.getMetricVersion());
    }

    @Test
    public void testSelectById_NotFound() {
        // Act
        InsightResult found = insightResultMapper.selectById(99999L);

        // Assert
        assertNull(found);
    }

    @Test
    public void testSelectById_ReturnsFullRecordWithAllFields() {
        // Arrange
        InsightResult record = createTestResult("2026-H1", "full_test");
        record.setStatus("failed");
        record.setErrorMessage("LLM timeout");
        record.setValidated(false);
        insightResultMapper.insertOrUpdate(record);

        // Act
        InsightResult found = insightResultMapper.selectById(record.getId());

        // Assert
        assertNotNull(found);
        assertEquals("failed", found.getStatus());
        assertEquals("LLM timeout", found.getErrorMessage());
        assertFalse(found.getValidated());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    // ========== selectMatchingScenarioCache ==========

    @Test
    public void testSelectMatchingScenarioCache_FoundReadyRecords() {
        // Arrange
        InsightResult r1 = createTestResult("2026-03-YTD", "c1");
        r1.setInsightType("scenario");
        r1.setMetricVersion("m1");
        r1.setPromptVersion("p1");
        r1.setStatus("ready");
        insightResultMapper.insertOrUpdate(r1);

        InsightResult r2 = createTestResult("2026-03-YTD", "c2");
        r2.setInsightType("scenario");
        r2.setMetricVersion("m1");
        r2.setPromptVersion("p1");
        r2.setStatus("ready");
        insightResultMapper.insertOrUpdate(r2);

        // Also add a non-ready one (should be filtered out)
        InsightResult r3 = createTestResult("2026-03-YTD", "c3");
        r3.setInsightType("scenario");
        r3.setMetricVersion("m1");
        r3.setPromptVersion("p1");
        r3.setStatus("pending");
        insightResultMapper.insertOrUpdate(r3);

        // Act
        List<InsightResult> results = insightResultMapper.selectMatchingScenarioCache(
                "2026-03-YTD", "m1", "p1");

        // Assert - only ready records returned, max 50
        assertNotNull(results);
        assertEquals(2, results.size());
        for (InsightResult r : results) {
            assertEquals("ready", r.getStatus());
            assertEquals("scenario", r.getInsightType());
        }
    }

    @Test
    public void testSelectMatchingScenarioCache_EmptyResult() {
        // Arrange - no scenario-type records at all
        InsightResult r = createTestResult("2026-01", "cx");
        r.setInsightType("card");  // not scenario type
        insightResultMapper.insertOrUpdate(r);

        // Act
        List<InsightResult> results = insightResultMapper.selectMatchingScenarioCache(
                "2026-01", "m1", "p1");

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ========== updateStatus ==========

    @Test
    public void testUpdateStatus_Success() {
        // Arrange
        InsightResult record = createTestResult("2026-03-YTD", "upd_test");
        insightResultMapper.insertOrUpdate(record);

        // Act
        int updated = insightResultMapper.updateStatus(record.getId(), "ready", null);

        // Assert
        assertEquals(1, updated);
        InsightResult found = insightResultMapper.selectById(record.getId());
        assertEquals("ready", found.getStatus());
    }

    @Test
    public void testUpdateStatus_WithErrorMessage() {
        // Arrange
        InsightResult record = createTestResult("2026-Q1", "err_test");
        insightResultMapper.insertOrUpdate(record);

        // Act
        int updated = insightResultMapper.updateStatus(record.getId(), "failed", "Schema validation failed");

        // Assert
        assertEquals(1, updated);
        InsightResult found = insightResultMapper.selectById(record.getId());
        assertEquals("failed", found.getStatus());
        assertEquals("Schema validation failed", found.getErrorMessage());
    }

    @Test
    public void testUpdateStatus_NonExistentId() {
        // Act
        int updated = insightResultMapper.updateStatus(99999L, "ready", null);

        // Assert - no rows affected
        assertEquals(0, updated);
    }

    // ========== selectByPeriodAndType ==========

    @Test
    public void testSelectByPeriodAndType_MultipleRecords() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            InsightResult r = createTestResult("2026-H1", "type_test_" + i);
            r.setInsightType("card");
            insightResultMapper.insertOrUpdate(r);
        }
        // Add a different type (should not be returned)
        InsightResult other = createTestResult("2026-H1", "other_type");
        other.setInsightType("scenario");
        insightResultMapper.insertOrUpdate(other);

        // Act
        List<InsightResult> results = insightResultMapper.selectByPeriodAndType(
                "2026-H1", "mv1", "pv1", "card");

        // Assert
        assertNotNull(results);
        assertEquals(3, results.size());
        for (InsightResult r : results) {
            assertEquals("card", r.getInsightType());
            assertEquals("2026-H1", r.getPeriod());
        }
    }

    @Test
    public void testSelectByPeriodAndType_NoMatches() {
        // Act
        List<InsightResult> results = insightResultMapper.selectByPeriodAndType(
                "2099-12", "nomv", "nopv", "none");

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ========== Multiple inserts and ordering ==========

    @Test
    public void testMultipleInserts_GenerateDistinctIds() {
        // Arrange & Act
        InsightResult r1 = createTestResult("P1", "id_test_1");
        InsightResult r2 = createTestResult("P1", "id_test_2");
        InsightResult r3 = createTestResult("P1", "id_test_3");

        insightResultMapper.insertOrUpdate(r1);
        insightResultMapper.insertOrUpdate(r2);
        insightResultMapper.insertOrUpdate(r3);

        // Assert - all IDs must be distinct
        assertNotNull(r1.getId());
        assertNotNull(r2.getId());
        assertNotNull(r3.getId());
        assertNotEquals(r1.getId(), r2.getId());
        assertNotEquals(r2.getId(), r3.getId());
        assertNotEquals(r1.getId(), r3.getId());

        // Verify all can be fetched back
        assertNotNull(insightResultMapper.selectById(r1.getId()));
        assertNotNull(insightResultMapper.selectById(r2.getId()));
        assertNotNull(insightResultMapper.selectById(r3.getId()));
    }
}
