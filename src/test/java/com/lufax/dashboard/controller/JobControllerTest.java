package com.lufax.dashboard.controller;

import com.lufax.dashboard.TestDataFactory;
import com.lufax.dashboard.model.dto.JobStatusDto;
import com.lufax.dashboard.model.request.CreateJobRequest;
import com.lufax.dashboard.model.response.CreateJobResponse;
import com.lufax.dashboard.service.InsightJobService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JobController}.
 * Covers all 5 endpoints: POST /create, GET /status/{id}, POST /cancel/{id}, DELETE /{id}, GET /list
 */
public class JobControllerTest {

    @Mock
    private InsightJobService jobService;

    @InjectMocks
    private JobController jobController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    // ========== POST /api/job/create ==========

    @Test
    public void testCreateJob_Success() {
        // Arrange
        CreateJobRequest request = TestDataFactory.createJobRequest(3);
        when(jobService.createJob(any(CreateJobRequest.class))).thenReturn(100L);
        doNothing().when(jobService).processJobAsync(anyLong());

        // Act
        ResponseEntity<CreateJobResponse> response = jobController.createJob(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        CreateJobResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(Long.valueOf(100L), body.getJobId());
        assertEquals("Job created successfully", body.getMessage());
        verify(jobService).createJob(request);
        verify(jobService).processJobAsync(100L);
    }

    @Test
    public void testCreateJob_SingleItem() {
        // Arrange
        CreateJobRequest request = TestDataFactory.createJobRequest(1);
        when(jobService.createJob(any(CreateJobRequest.class))).thenReturn(200L);
        doNothing().when(jobService).processJobAsync(anyLong());

        // Act
        ResponseEntity<CreateJobResponse> response = jobController.createJob(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Long.valueOf(200L), response.getBody().getJobId());
    }

    @Test
    public void testCreateJob_ManyItems() {
        // Arrange
        CreateJobRequest request = TestDataFactory.createJobRequest(10);
        when(jobService.createJob(any(CreateJobRequest.class))).thenReturn(300L);
        doNothing().when(jobService).processJobAsync(anyLong());

        // Act
        ResponseEntity<CreateJobResponse> response = jobController.createJob(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(jobService).createJob(argThat(req -> req.getItems().size() == 10));
        verify(jobService).processJobAsync(300L);
    }

    @Test
    public void testCreateJob_TriggersAsyncProcessing() {
        // Arrange - verify async is called after creation
        CreateJobRequest request = TestDataFactory.createJobRequest(2);
        when(jobService.createJob(any(CreateJobRequest.class))).thenReturn(42L);

        // Act
        jobController.createJob(request);

        // Assert - processJobAsync must be called with the returned jobId
        verify(jobService).processJobAsync(42L);
    }

    // ========== GET /api/job/status/{job_id} ==========

    @Test
    public void testGetJobStatus_Found() {
        // Arrange
        Long jobId = 1L;
        JobStatusDto status = new JobStatusDto();
        status.setJobId(jobId);
        status.setStatus("PROCESSING");
        status.setTotalCount(5);
        status.setFinishedCount(2);
        status.setFailedCount(0);
        when(jobService.getJobStatus(jobId)).thenReturn(status);

        // Act
        ResponseEntity<JobStatusDto> response = jobController.getJobStatus(jobId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(jobId.toString(), response.getBody().getJobId());
        assertEquals("PROCESSING", response.getBody().getStatus());
        assertEquals(Integer.valueOf(5), response.getBody().getTotalCount());
        verify(jobService).getJobStatus(jobId);
    }

    @Test
    public void testGetJobStatus_CompletedJob() {
        // Arrange
        JobStatusDto status = new JobStatusDto();
        status.setJobId(99L);
        status.setStatus("COMPLETED");
        status.setTotalCount(3);
        status.setFinishedCount(3);
        status.setFailedCount(0);
        when(jobService.getJobStatus(99L)).thenReturn(status);

        // Act
        ResponseEntity<JobStatusDto> response = jobController.getJobStatus(99L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("COMPLETED", response.getBody().getStatus());
        assertEquals(Integer.valueOf(3), response.getBody().getFinishedCount());
    }

    @Test
    public void testGetJobStatus_NotFound() {
        // Arrange
        Long jobId = 999L;
        when(jobService.getJobStatus(jobId)).thenReturn(null);

        // Act
        ResponseEntity<JobStatusDto> response = jobController.getJobStatus(jobId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ========== POST /api/job/cancel/{job_id} ==========

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelJob_Success() {
        // Arrange
        Long jobId = 1L;
        when(jobService.cancelJob(jobId)).thenReturn(true);

        // Act
        ResponseEntity<Map<String, Object>> response = jobController.cancelJob(jobId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals("Job cancelled", response.getBody().get("message"));
        verify(jobService).cancelJob(jobId);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelJob_CannotCancel() {
        // Arrange - job already running or completed
        Long jobId = 2L;
        when(jobService.cancelJob(jobId)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = jobController.cancelJob(jobId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        assertEquals("Cannot cancel job", response.getBody().get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCancelJob_NonExistentJob() {
        // Arrange
        Long jobId = 888L;
        when(jobService.cancelJob(jobId)).thenReturn(false);

        // Act
        ResponseEntity<Map<String, Object>> response = jobController.cancelJob(jobId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse((Boolean) response.getBody().get("success"));
    }

    // ========== DELETE /api/job/{job_id} ==========

    @Test
    public void testDeleteJob_Success() {
        // Arrange
        Long jobId = 1L;
        doNothing().when(jobService).deleteJob(jobId);

        // Act
        ResponseEntity<Void> response = jobController.deleteJob(jobId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(jobService).deleteJob(jobId);
    }

    @Test
    public void testDeleteJob_VerifyServiceCalledWithCorrectId() {
        // Arrange
        Long jobId = 55L;
        doNothing().when(jobService).deleteJob(anyLong());

        // Act
        jobController.deleteJob(jobId);

        // Assert
        verify(jobService).deleteJob(55L);
    }

    // ========== GET /api/job/list ==========

    @Test
    public void testListJobs_DefaultPagination() {
        // Arrange
        List<JobStatusDto> expectedJobs = Arrays.asList(
                createStatusDto(1L, "COMPLETED"),
                createStatusDto(2L, "PENDING")
        );
        when(jobService.listJobs(1, 10)).thenReturn(expectedJobs);

        // Act
        ResponseEntity<List<JobStatusDto>> response = jobController.listJobs(1, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(jobService).listJobs(1, 10);
    }

    @Test
    public void testListJobs_CustomPage() {
        // Arrange - page 2, size 5
        List<JobStatusDto> expectedJobs = Collections.singletonList(
                createStatusDto(6L, "PROCESSING")
        );
        when(jobService.listJobs(2, 5)).thenReturn(expectedJobs);

        // Act
        ResponseEntity<List<JobStatusDto>> response = jobController.listJobs(2, 5);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("6", response.getBody().get(0).getJobId());
        verify(jobService).listJobs(2, 5);
    }

    @Test
    public void testListJobs_EmptyResult() {
        // Arrange
        when(jobService.listJobs(99, 10)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<JobStatusDto>> response = jobController.listJobs(99, 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void testListJobs_LargePageSize() {
        // Arrange
        List<JobStatusDto> jobs = Arrays.asList(
                createStatusDto(1L, "COMPLETED"),
                createStatusDto(2L, "FAILED"),
                createStatusDto(3L, "COMPLETED")
        );
        when(jobService.listJobs(1, 50)).thenReturn(jobs);

        // Act
        ResponseEntity<List<JobStatusDto>> response = jobController.listJobs(1, 50);

        // Assert
        assertEquals(3, response.getBody().size());
        verify(jobService).listJobs(1, 50);
    }

    // ========== Helper Methods ==========

    private JobStatusDto createStatusDto(Long id, String statusStr) {
        JobStatusDto dto = new JobStatusDto();
        dto.setJobId(id);
        dto.setStatus(statusStr);
        dto.setTotalCount(3);
        dto.setFinishedCount("COMPLETED".equals(statusStr) ? 3 : 0);
        dto.setFailedCount("FAILED".equals(statusStr) ? 3 : 0);
        return dto;
    }
}
