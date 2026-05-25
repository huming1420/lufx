package com.lufax.dashboard.repository;

import com.lufax.dashboard.model.entity.InsightJobItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for {@link InsightJobItemMapper}.
 * Uses H2 in-memory database with PostgreSQL compatibility mode.
 * All tests run within a transaction that rolls back after each test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InsightJobItemMapperTest {

    @Autowired
    private InsightJobItemMapper insightJobItemMapper;

    // ========== Helper: create a valid InsightJobItem entity ==========
    private InsightJobItem createTestItem(String jobId, String scenario) {
        InsightJobItem item = new InsightJobItem();
        item.setJobId(jobId);
        item.setScenario(scenario);
        item.setStatus("PENDING");
        return item;
    }

    // ========== insert ==========

    @Test
    public void testInsert_Success() {
        // Arrange
        InsightJobItem item = createTestItem("JOB-001", "base");

        // Act
        int rows = insightJobItemMapper.insert(item);

        // Assert
        assertEquals(1, rows);
        assertNotNull(item.getId());
        assertTrue(item.getId() > 0);
    }

    @Test
    public void testInsert_PreservesAllFields() {
        // Arrange
        InsightJobItem item = new InsightJobItem();
        item.setJobId("JOB-FIELDS");
        item.setInsightType("scenario_insight");
        item.setCardId("profit_analysis");
        item.setScenario("base");
        item.setMetricDate("2026-03-20");
        item.setStatus("PENDING");

        // Act
        insightJobItemMapper.insert(item);

        // Assert - retrieve and verify all fields
        List<InsightJobItem> items = insightJobItemMapper.selectByJobId("JOB-FIELDS");
        assertNotNull(items);
        assertEquals(1, items.size());
        InsightJobItem found = items.get(0);
        assertEquals("scenario_insight", found.getInsightType());
        assertEquals("profit_analysis", found.getCardId());
        assertEquals("base", found.getScenario());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    @Test
    public void testInsert_SetsTimestamps() {
        // Arrange
        InsightJobItem item = createTestItem("JOB-TS", "bear");

        // Act
        insightJobItemMapper.insert(item);

        // Assert
        List<InsightJobItem> items = insightJobItemMapper.selectByJobId("JOB-TS");
        assertFalse(items.isEmpty());
        assertNotNull(items.get(0).getCreatedAt());
        assertNotNull(items.get(0).getUpdatedAt());
    }

    @Test
    public void testInsert_MultipleItemsSameJob() {
        // Arrange & Act - insert 3 items for the same job
        String[] scenarios = {"base", "bear", "bull"};
        for (String s : scenarios) {
            InsightJobItem item = createTestItem("JOB-MULTI", s);
            insightJobItemMapper.insert(item);
        }

        // Assert - retrieve all by job_id
        List<InsightJobItem> items = insightJobItemMapper.selectByJobId("JOB-MULTI");
        assertEquals(3, items.size());
    }

    // ========== selectByJobId ==========

    @Test
    public void testSelectByJobId_Found() {
        // Arrange
        InsightJobItem item1 = createTestItem("JOB-SLCT", "base");
        insightJobItemMapper.insert(item1);

        InsightJobItem item2 = createTestItem("JOB-SLCT", "bear");
        item2.setMetricDate("2026-03-21");
        insightJobItemMapper.insert(item2);

        // Act
        List<InsightJobItem> items = insightJobItemMapper.selectByJobId("JOB-SLCT");

        // Assert
        assertNotNull(items);
        assertEquals(2, items.size());
    }

    @Test
    public void testSelectByJobId_NotFound() {
        // Act
        List<InsightJobItem> items = insightJobItemMapper.selectByJobId("NO-SUCH-JOB");

        // Assert
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    public void testSelectByJobId_OrderedById() {
        // Arrange - insert multiple to test ordering
        InsightJobItem item1 = createTestItem("JOB-ORD", "first");
        insightJobItemMapper.insert(item1);

        InsightJobItem item2 = createTestItem("JOB-ORD", "second");
        insightJobItemMapper.insert(item2);

        InsightJobItem item3 = createTestItem("JOB-ORD", "third");
        insightJobItemMapper.insert(item3);

        // Act
        List<InsightJobItem> items = insightJobItemMapper.selectByJobId("JOB-ORD");

        // Assert - should be ordered by id ascending
        assertEquals(3, items.size());
        assertTrue(items.get(0).getId() < items.get(1).getId());
        assertTrue(items.get(1).getId() < items.get(2).getId());
    }

    @Test
    public void testSelectByJobId_DoesNotCrossJobs() {
        // Arrange - items for two different jobs
        insightJobItemMapper.insert(createTestItem("JOB-A", "scenario_a"));
        insightJobItemMapper.insert(createTestItem("JOB-A", "scenario_a_2"));
        insightJobItemMapper.insert(createTestItem("JOB-B", "scenario_b"));

        // Act - query only JOB-A
        List<InsightJobItem> itemsA = insightJobItemMapper.selectByJobId("JOB-A");

        // Assert
        assertEquals(2, itemsA.size());
        for (InsightJobItem item : itemsA) {
            assertEquals("JOB-A", item.getJobId());
        }
    }

    // ========== selectPendingByJobId ==========

    @Test
    public void testSelectPendingByJobId_ReturnsOnlyPendingAndRunning() {
        // Arrange
        InsightJobItem pending = createTestItem("JOB-PND", "pending_item");
        pending.setStatus("pending");
        insightJobItemMapper.insert(pending);

        InsightJobItem running = createTestItem("JOB-PND", "running_item");
        running.setStatus("running");
        insightJobItemMapper.insert(running);

        InsightJobItem completed = createTestItem("JOB-PND", "completed_item");
        completed.setStatus("completed");   // should be excluded
        insightJobItemMapper.insert(completed);

        InsightJobItem failed = createTestItem("JOB-PND", "failed_item");
        failed.setStatus("failed");         // should be excluded
        insightJobItemMapper.insert(failed);

        // Act
        List<InsightJobItem> pendingItems = insightJobItemMapper.selectPendingByJobId("JOB-PND");

        // Assert
        assertNotNull(pendingItems);
        assertEquals(2, pendingItems.size());
        for (InsightJobItem item : pendingItems) {
            String status = item.getStatus();
            assertTrue("Expected pending or running, got: " + status,
                    "pending".equalsIgnoreCase(status) || "running".equalsIgnoreCase(status));
        }
    }

    @Test
    public void testSelectPendingByJobId_AllCompleted() {
        // Arrange - all items completed
        InsightJobItem item1 = createTestItem("JOB-DONE", "done1");
        item1.setStatus("completed");
        insightJobItemMapper.insert(item1);

        InsightJobItem item2 = createTestItem("JOB-DONE", "done2");
        item2.setStatus("failed");
        insightJobItemMapper.insert(item2);

        // Act
        List<InsightJobItem> pendingItems = insightJobItemMapper.selectPendingByJobId("JOB-DONE");

        // Assert
        assertNotNull(pendingItems);
        assertTrue(pendingItems.isEmpty());
    }

    @Test
    public void testSelectPendingByJobId_NoSuchJob() {
        // Act
        List<InsightJobItem> items = insightJobItemMapper.selectPendingByJobId("GHOST-JOB");

        // Assert
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    // ========== updateStatus ==========

    @Test
    public void testUpdateStatus_BasicStatusChange() {
        // Arrange
        InsightJobItem item = createTestItem("JOB-UPD", "upd_scenario");
        insightJobItemMapper.insert(item);
        Integer itemId = item.getId();

        // Act
        int rows = insightJobItemMapper.updateStatus(itemId, "running");

        // Assert
        assertEquals(1, rows);
        List<InsightJobItem> items = insightJobItemMapper.selectByJobId("JOB-UPD");
        InsightJobItem updated = items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst().orElse(null);
        assertNotNull(updated);
        assertEquals("running", updated.getStatus());
    }

    @Test
    public void testUpdateStatus_CompleteWithResultAndError() {
        // Arrange
        InsightJobItem item = createTestItem("JOB-CMPLT", "cmplt_scn");
        insightJobItemMapper.insert(item);
        Integer itemId = item.getId();

        // Act - mark complete with result ID and no error
        int rows = insightJobItemMapper.updateStatusFull(itemId, "completed", 42, null);

        // Assert
        assertEquals(1, rows);
        List<InsightJobItem> items = insightJobItemMapper.selectByJobId("JOB-CMPLT");
        InsightJobItem updated = items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst().orElse(null);
        assertNotNull(updated);
        assertEquals("completed", updated.getStatus());
        assertEquals(Integer.valueOf(42), updated.getResultId());
        assertNull(updated.getErrorMessage());
    }

    @Test
    public void testUpdateStatus_FailedWithError() {
        // Arrange
        InsightJobItem item = createTestItem("JOB-FAIL", "fail_scn");
        insightJobItemMapper.insert(item);
        Integer itemId = item.getId();

        // Act - mark failed with error message
        int rows = insightJobItemMapper.updateStatusFull(itemId, "failed", null, "LLM call timed out");

        // Assert
        assertEquals(1, rows);
        List<InsightJobItem> items = insightJobItemMapper.selectByJobId("JOB-FAIL");
        InsightJobItem updated = items.stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst().orElse(null);
        assertNotNull(updated);
        assertEquals("failed", updated.getStatus());
        assertEquals("LLM call timed out", updated.getErrorMessage());
        assertNull(updated.getResultId());
    }

    @Test
    public void testUpdateStatus_NonExistentId() {
        // Act
        int rows = insightJobItemMapper.updateStatus(99999, "running");

        // Assert - no rows affected
        assertEquals(0, rows);
    }

    // ========== Lifecycle: full item workflow ==========

    @Test
    public void testFullLifecycle_PendingToCompleted() {
        // Arrange & Step 1: Insert as PENDING
        InsightJobItem item = createTestItem("JOB-LIFE", "life_scenario");
        item.setStatus("PENDING");
        insightJobItemMapper.insert(item);
        Integer id = item.getId();

        // Verify initial state
        List<InsightJobItem> pending = insightJobItemMapper.selectPendingByJobId("JOB-LIFE");
        assertEquals(1, pending.size());
        assertEquals("PENDING", pending.get(0).getStatus());

        // Step 2: Update to RUNNING
        insightJobItemMapper.updateStatus(id, "RUNNING");
        List<InsightJobItem> stillPending = insightJobItemMapper.selectPendingByJobId("JOB-LIFE");
        assertEquals(1, stillPending.size());  // running is still "active"
        assertEquals("RUNNING", stillPending.get(0).getStatus());

        // Step 3: Update to COMPLETED with result
        insightJobItemMapper.updateStatusFull(id, "COMPLETED", 100, null);
        List<InsightJobItem> noMorePending = insightJobItemMapper.selectPendingByJobId("JOB-LIFE");
        assertTrue(noMorePending.isEmpty());

        // Verify final state
        List<InsightJobItem> allItems = insightJobItemMapper.selectByJobId("JOB-LIFE");
        assertEquals(1, allItems.size());
        assertEquals("COMPLETED", allItems.get(0).getStatus());
        assertEquals(Integer.valueOf(100), allItems.get(0).getResultId());
    }

    @Test
    public void testFullLifecycle_PendingToFailed() {
        // Arrange & Step 1: Insert
        InsightJobItem item = createTestItem("JOB-FAIL-Life", "fail_life");
        item.setStatus("PENDING");
        insightJobItemMapper.insert(item);
        Integer id = item.getId();

        // Step 2: Update to FAILED
        insightJobItemMapper.updateStatusFull(id, "FAILED", null, "Connection refused");

        // Verify
        List<InsightJobItem> pending = insightJobItemMapper.selectPendingByJobId("JOB-FAIL-Life");
        assertTrue(pending.isEmpty());

        List<InsightJobItem> all = insightJobItemMapper.selectByJobId("JOB-FAIL-Life");
        assertEquals(1, all.size());
        assertEquals("FAILED", all.get(0).getStatus());
        assertEquals("Connection refused", all.get(0).getErrorMessage());
    }
}
