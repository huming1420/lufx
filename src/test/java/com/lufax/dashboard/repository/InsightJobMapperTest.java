package com.lufax.dashboard.repository;

import com.lufax.dashboard.model.entity.InsightJob;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for {@link InsightJobMapper}.
 * Uses H2 in-memory database with PostgreSQL compatibility mode.
 * All tests run within a transaction that rolls back after each test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InsightJobMapperTest {

    @Autowired
    private InsightJobMapper insightJobMapper;

    // ========== Helper: create a valid InsightJob entity ==========
    private InsightJob createTestJob(String jobIdStr) {
        InsightJob job = new InsightJob();
        job.setJobId(jobIdStr);
        job.setPeriod("2026-03-YTD");
        job.setJobType("scenario_insight");
        job.setStatus("PENDING");
        job.setTotalCount(3);
        job.setFinishedCount(0);
        job.setFailedCount(0);
        return job;
    }

    // ========== insert ==========

    @Test
    public void testInsert_Success() {
        // Arrange
        InsightJob job = createTestJob("JOB-001");

        // Act
        int rows = insightJobMapper.insert(job);

        // Assert
        assertEquals(1, rows);
    }

    @Test
    public void testInsert_PreservesAllFields() {
        // Arrange
        InsightJob job = createTestJob("JOB-FIELDS");
        job.setMetricVersion("mv-1.0");
        job.setPromptVersion("pv-2.0");
        job.setTotalCount(10);
        job.setFinishedCount(3);
        job.setFailedCount(1);
        job.setForceRefresh(true);

        // Act
        insightJobMapper.insert(job);

        // Assert - retrieve and verify all fields
        InsightJob found = insightJobMapper.selectById("JOB-FIELDS");
        assertNotNull(found);
        assertEquals("2026-03-YTD", found.getPeriod());
        assertEquals("scenario_insight", found.getJobType());
        assertEquals("PENDING", found.getStatus());
        assertEquals(Integer.valueOf(10), found.getTotalCount());
        assertEquals(Integer.valueOf(3), found.getFinishedCount());
        assertEquals(Integer.valueOf(1), found.getFailedCount());
        assertTrue(found.getForceRefresh());
        assertNotNull(found.getCreatedAt());
    }

    @Test
    public void testInsert_SetsCreatedAt() {
        // Arrange
        InsightJob job = createTestJob("JOB-TS");

        // Act
        insightJobMapper.insert(job);

        // Assert
        InsightJob found = insightJobMapper.selectById("JOB-TS");
        assertNotNull(found.getCreatedAt());
    }

    // ========== selectById ==========

    @Test
    public void testSelectById_Found() {
        // Arrange
        InsightJob job = createTestJob("JOB-FIND");
        insightJobMapper.insert(job);

        // Act
        InsightJob found = insightJobMapper.selectById("JOB-FIND");

        // Assert
        assertNotNull(found);
        assertEquals("JOB-FIND", found.getJobId());
        assertEquals("PENDING", found.getStatus());
        assertEquals(Integer.valueOf(3), found.getTotalCount());
    }

    @Test
    public void testSelectById_NotFound() {
        // Act
        InsightJob found = insightJobMapper.selectById("NONEXISTENT");

        // Assert
        assertNull(found);
    }

    // ========== updateStatus (full version with jobId) ==========

    @Test
    public void testUpdateStatus_ByJobId_AllFields() {
        // Arrange
        InsightJob job = createTestJob("JOB-UPD1");
        insightJobMapper.insert(job);

        // Act
        int rows = insightJobMapper.updateStatus(
                "JOB-UPD1", "RUNNING", 1, 0, null, "2026-03-20T10:05:00Z", null);

        // Assert
        assertEquals(1, rows);
        InsightJob found = insightJobMapper.selectById("JOB-UPD1");
        assertEquals("RUNNING", found.getStatus());
        assertEquals(Integer.valueOf(1), found.getFinishedCount());
        assertEquals(Integer.valueOf(0), found.getFailedCount());
        assertEquals("2026-03-20T10:05:00Z", found.getStartedAt());
    }

    @Test
    public void testUpdateStatus_ByJobId_Complete() {
        // Arrange
        InsightJob job = createTestJob("JOB-UPD2");
        insightJobMapper.insert(job);

        // Act - mark as completed
        int rows = insightJobMapper.updateStatus(
                "JOB-UPD2", "COMPLETED", 3, 0, null, "2026-03-20T10:00:00Z", "2026-03-20T10:10:00Z");

        // Assert
        assertEquals(1, rows);
        InsightJob found = insightJobMapper.selectById("JOB-UPD2");
        assertEquals("COMPLETED", found.getStatus());
        assertEquals(Integer.valueOf(3), found.getFinishedCount());
        assertEquals("2026-03-20T10:10:00Z", found.getFinishedAt());
    }

    @Test
    public void testUpdateStatus_ByJobId_WithErrorMessage() {
        // Arrange
        InsightJob job = createTestJob("JOB-ERR");
        insightJobMapper.insert(job);

        // Act - mark as failed
        int rows = insightJobMapper.updateStatus(
                "JOB-ERR", "FAILED", 0, 1, "LLM service timeout", null, null);

        // Assert
        assertEquals(1, rows);
        InsightJob found = insightJobMapper.selectById("JOB-ERR");
        assertEquals("FAILED", found.getStatus());
        assertEquals(Integer.valueOf(1), found.getFailedCount());
        assertEquals("LLM service timeout", found.getErrorMessage());
    }

    @Test
    public void testUpdateStatus_ByJobId_NullOptionalFields() {
        // Arrange
        InsightJob job = createTestJob("JOB-Nullopt");
        insightJobMapper.insert(job);

        // Act - only update status and error, leave counts/timestamps null (dynamic SQL <if> skips them)
        int rows = insightJobMapper.updateStatus(
                "JOB-Nullopt", "PROCESSING", null, null, "warning", null, null);

        // Assert
        assertEquals(1, rows);
        InsightJob found = insightJobMapper.selectById("JOB-Nullopt");
        assertEquals("PROCESSING", found.getStatus());
        assertEquals("warning", found.getErrorMessage());
        // Counts should remain at original values (not updated because null)
        assertEquals(Integer.valueOf(0), found.getFinishedCount());
    }

    @Test
    public void testUpdateStatus_NonExistentJob() {
        // Act
        int rows = insightJobMapper.updateStatus(
                "NO-SUCH-JOB", "RUNNING", 0, 0, null, null, null);

        // Assert - no rows affected
        assertEquals(0, rows);
    }

    // ========== selectActiveByPeriod ==========

    @Test
    public void testSelectActiveByPeriod_ReturnsPendingAndRunning() {
        // Arrange
        InsightJob j1 = createTestJob("ACT-1");
        j1.setStatus("pending");
        insightJobMapper.insert(j1);

        InsightJob j2 = createTestJob("ACT-2");
        j2.setStatus("running");
        insightJobMapper.insert(j2);

        InsightJob j3 = createTestJob("ACT-3");
        j3.setStatus("completed");   // should NOT be returned
        insightJobMapper.insert(j3);

        InsightJob j4 = createTestJob("ACT-4");
        j4.setStatus("failed");      // should NOT be returned
        insightJobMapper.insert(j4);

        // Act
        List<InsightJob> activeJobs = insightJobMapper.selectActiveByPeriod("2026-03-YTD");

        // Assert
        assertNotNull(activeJobs);
        assertEquals(2, activeJobs.size());
        for (InsightJob j : activeJobs) {
            assertTrue("Expected pending or running, got: " + j.getStatus(),
                    "pending".equalsIgnoreCase(j.getStatus()) || "running".equalsIgnoreCase(j.getStatus()));
        }
    }

    @Test
    public void testSelectActiveByPeriod_NoActiveJobs() {
        // Arrange - only completed/failed jobs exist
        InsightJob j1 = createTestJob("DONE-1");
        j1.setStatus("completed");
        insightJobMapper.insert(j1);

        // Act
        List<InsightJob> activeJobs = insightJobMapper.selectActiveByPeriod("2026-03-YTD");

        // Assert
        assertNotNull(activeJobs);
        assertTrue(activeJobs.isEmpty());
    }

    @Test
    public void testSelectActiveByPeriod_DifferentPeriod() {
        // Arrange - jobs from different periods
        InsightJob j1 = createTestJob("P1-JOB");
        j1.setPeriod("2026-Q1");
        j1.setStatus("pending");
        insightJobMapper.insert(j1);

        InsightJob j2 = createTestJob("P2-JOB");
        j2.setPeriod("2026-Q2");
        j2.setStatus("running");
        insightJobMapper.insert(j2);

        // Act - query Q1 only
        List<InsightJob> q1Jobs = insightJobMapper.selectActiveByPeriod("2026-Q1");

        // Assert
        assertEquals(1, q1Jobs.size());
        assertEquals("P1-JOB", q1Jobs.get(0).getJobId());
    }

    // ========== Multiple inserts ==========

    @Test
    public void testMultipleInserts_SamePeriodDifferentJobIds() {
        // Arrange & Act
        String[] jobIds = {"BATCH-001", "BATCH-002", "BATCH-003"};
        for (String jid : jobIds) {
            InsightJob job = createTestJob(jid);
            insightJobMapper.insert(job);
        }

        // Assert - all three can be retrieved
        for (String jid : jobIds) {
            InsightJob found = insightJobMapper.selectById(jid);
            assertNotNull(found);
            assertEquals(jid, found.getJobId());
        }
    }

    @Test
    public void testInsert_JobWithMinimalFields() {
        // Arrange - only required fields
        InsightJob job = new InsightJob();
        job.setJobId("MINIMAL");
        job.setPeriod("2026-M01");
        job.setJobType("simple");
        job.setStatus("PENDING");

        // Act
        int rows = insightJobMapper.insert(job);

        // Assert
        assertEquals(1, rows);
        InsightJob found = insightJobMapper.selectById("MINIMAL");
        assertNotNull(found);
        assertEquals("MINIMAL", found.getJobId());
        assertEquals("simple", found.getJobType());
    }
}
