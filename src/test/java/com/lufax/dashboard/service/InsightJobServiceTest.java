package com.lufax.dashboard.service;

import com.lufax.dashboard.model.dto.JobStatusDto;
import com.lufax.dashboard.model.entity.InsightJob;
import com.lufax.dashboard.model.entity.InsightJobItem;
import com.lufax.dashboard.model.request.CreateJobRequest;
import com.lufax.dashboard.repository.InsightJobItemMapper;
import com.lufax.dashboard.repository.InsightJobMapper;
import com.lufax.dashboard.TestDataFactory;
import com.lufax.dashboard.util.JsonUtils;
import com.lufax.dashboard.util.TimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InsightJobService 单元测试 (Mock)
 * 覆盖: Job 创建、状态查询、取消、删除、列表分页
 */
public class InsightJobServiceTest {

    @Mock
    private InsightJobMapper jobMapper;

    @Mock
    private InsightJobItemMapper jobItemMapper;

    @Mock
    private InsightService insightService;

    @Mock
    private JsonUtils jsonUtils;

    @Mock
    private TimeUtils timeUtils;

    @InjectMocks
    private InsightJobService jobService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(timeUtils.getCurrentTimeIso8601()).thenReturn("2025-06-15T10:00:00Z");

        // 默认 insert 后设置 ID（模拟数据库自增）
        doAnswer(invocation -> {
            InsightJob job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(1L);
            }
            return null;
        }).when(jobMapper).insert(any(InsightJob.class));

        doAnswer(invocation -> {
            InsightJobItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId((int) (Math.random() * 1000));
            }
            return null;
        }).when(jobItemMapper).insert(any(InsightJobItem.class));
    }

    // ========== createJob ==========

    @Test
    public void testCreateJob_ReturnsNonNullId() {
        CreateJobRequest request = TestDataFactory.createJobRequest(3);

        Long jobId = jobService.createJob(request);

        assertNotNull("创建的 jobId 不应为 null", jobId);
        assertTrue("jobId 应为正数", jobId > 0);
        verify(jobMapper, times(1)).insert(any(InsightJob.class));
        verify(jobItemMapper, times(3)).insert(any(InsightJobItem.class)); // 3 items
    }

    @Test
    public void testCreateJob_SetsPendingStatus() {
        CreateJobRequest request = TestDataFactory.createJobRequest(2);

        ArgumentCaptor<InsightJob> captor = ArgumentCaptor.forClass(InsightJob.class);
        jobService.createJob(request);

        verify(jobMapper).insert(captor.capture());
        assertEquals("PENDING", captor.getValue().getStatus());
    }

    @Test
    public void testCreateJob_SetsTotalItemsCount() {
        CreateJobRequest request = TestDataFactory.createJobRequest(4);

        ArgumentCaptor<InsightJob> captor = ArgumentCaptor.forClass(InsightJob.class);
        jobService.createJob(request);

        assertEquals(Integer.valueOf(4), captor.getValue().getTotalItems());
    }

    @Test
    public void testCreateJob_EmptyItems_ZeroTotalItems() {
        CreateJobRequest request = new CreateJobRequest();
        request.setJobType("batch_insight");
        request.setItems(Collections.emptyList());

        ArgumentCaptor<InsightJob> captor = ArgumentCaptor.forClass(InsightJob.class);
        jobService.createJob(request);

        assertEquals(Integer.valueOf(0), captor.getValue().getTotalItems());
        verify(jobItemMapper, never()).insert(any(InsightJobItem.class));
    }

    @Test
    public void testCreateJob_NullItems_ZeroTotalItems() {
        CreateJobRequest request = new CreateJobRequest();
        request.setJobType("single");
        request.setItems(null);

        Long jobId = jobService.createJob(request);

        assertNotNull(jobId);
        verify(jobItemMapper, never()).insert(any(InsightJobItem.class));
    }

    @Test
    public void testCreateJob_InitializesStatusMap() {
        CreateJobRequest request = TestDataFactory.createJobRequest(1);

        Long jobId = jobService.createJob(request);

        // 创建后可通过 getJobStatus 查到
        JobStatusDto status = jobService.getJobStatus(jobId);
        assertNotNull(status);
        assertEquals("PENDING", status.getStatus());
        assertEquals(1, status.getTotal());
        assertEquals(0, status.getCompleted());
    }

    // ========== getJobStatus ==========

    @Test
    public void testGetJobStatus_FromStatusMap() {
        // 先创建一个 job（会写入 statusMap）
        CreateJobRequest request = TestDataFactory.createJobRequest(2);
        Long jobId = jobService.createJob(request);

        JobStatusDto status = jobService.getJobStatus(jobId);

        assertNotNull(status);
        assertEquals(String.valueOf(jobId), status.getJobId());
        assertEquals("PENDING", status.getStatus());
    }

    @Test
    public void testGetJobStatus_JobNotFound_ReturnsNull() {
        // 未创建过的 job ID
        when(jobMapper.selectById(eq("999"))).thenReturn(null);
        assertNull(jobService.getJobStatus(999L));
    }

    @Test
    public void testGetJobStatus_FromDbWhenNotInMap() {
        // 模拟不在内存 map 中但 DB 存在的情况
        InsightJob dbJob = new InsightJob();
        dbJob.setId(100L);
        dbJob.setStatus("COMPLETED");
        dbJob.setTotalItems(3);
        when(jobMapper.selectById(eq("100"))).thenReturn(dbJob);

        InsightJobItem completedItem = new InsightJobItem();
        completedItem.setStatus("COMPLETED");
        InsightJobItem failedItem = new InsightJobItem();
        failedItem.setStatus("FAILED");
        when(jobItemMapper.selectByJobId(eq("100")))
                .thenReturn(Arrays.asList(completedItem, completedItem, failedItem));

        JobStatusDto status = jobService.getJobStatus(100L);

        assertNotNull(status);
        assertEquals("COMPLETED", status.getStatus());
        assertEquals(2, status.getCompleted()); // 2 COMPLETED
        assertEquals(1, status.getFailed());     // 1 FAILED
    }

    // ========== cancelJob ==========

    @Test
    public void testCancelJob_PendingJob_Success() {
        InsightJob pendingJob = new InsightJob();
        pendingJob.setId(50L);
        pendingJob.setStatus("PENDING");
        when(jobMapper.selectById(eq("50"))).thenReturn(pendingJob);

        boolean result = jobService.cancelJob(50L);

        assertTrue("取消 PENDING 状态的 job 应成功", result);
        verify(jobMapper).updateStatus(50L, "CANCELLED");
    }

    @Test
    public void testCancelJob_ProcessingJob_Success() {
        InsightJob processingJob = new InsightJob();
        processingJob.setId(51L);
        processingJob.setStatus("PROCESSING");
        when(jobMapper.selectById(eq("51"))).thenReturn(processingJob);

        boolean result = jobService.cancelJob(51L);

        assertTrue("取消 PROCESSING 状态的 job 应成功", result);
    }

    @Test
    public void testCancelJob_CompletedJob_Fails() {
        InsightJob completedJob = new InsightJob();
        completedJob.setId(52L);
        completedJob.setStatus("COMPLETED");
        when(jobMapper.selectById(eq("52"))).thenReturn(completedJob);

        boolean result = jobService.cancelJob(52L);

        assertFalse("已完成的 job 不能取消", result);
        verify(jobMapper, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    public void testCancelJob_NonExistentJob_Fails() {
        when(jobMapper.selectById(eq("999"))).thenReturn(null);

        boolean result = jobService.cancelJob(999L);

        assertFalse("不存在的 job 不能取消", result);
    }

    // ========== deleteJob ==========

    @Test
    public void testDeleteJob_CompletedJob_Success() {
        InsightJob completedJob = new InsightJob();
        completedJob.setId(60L);
        completedJob.setStatus("COMPLETED");
        when(jobMapper.selectById(eq("60"))).thenReturn(completedJob);

        boolean result = jobService.deleteJob(60L);

        assertTrue("删除已完成的 job 应成功", result);
        verify(jobItemMapper).deleteByJobId(60L);
        verify(jobMapper).deleteById(60L);
    }

    @Test
    public void testDeleteJob_FailedJob_Success() {
        InsightJob failedJob = new InsightJob();
        failedJob.setId(61L);
        failedJob.setStatus("FAILED");
        when(jobMapper.selectById(eq("61"))).thenReturn(failedJob);

        boolean result = jobService.deleteJob(61L);

        assertTrue("删除失败的 job 应成功", result);
    }

    @Test
    public void testDeleteJob_PendingJob_Fails() {
        InsightJob pendingJob = new InsightJob();
        pendingJob.setId(62L);
        pendingJob.setStatus("PENDING");
        when(jobMapper.selectById(eq("62"))).thenReturn(pendingJob);

        boolean result = jobService.deleteJob(62L);

        assertFalse("不能删除进行中的 job", result);
    }

    @Test
    public void testDeleteJob_NonExistent_Fails() {
        when(jobMapper.selectById(eq("999"))).thenReturn(null);

        assertFalse(jobService.deleteJob(999L));
    }

    // ========== listJobs ==========

    @Test
    public void testListJobs_ReturnsJobList() {
        InsightJob job1 = new InsightJob();
        job1.setId(1L);
        job1.setStatus("COMPLETED");
        job1.setCreatedAt("2025-01-01T00:00:00Z");

        InsightJob job2 = new InsightJob();
        job2.setId(2L);
        job2.setStatus("PROCESSING");
        job2.setCreatedAt("2025-01-02T00:00:00Z");

        when(jobMapper.selectWithPagination(0, 20))
                .thenReturn(Arrays.asList(job1, job2));

        List<JobStatusDto> results = jobService.listJobs(0, 20);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("1", results.get(0).getJobId());
        assertEquals("COMPLETED", results.get(0).getStatus());
    }

    @Test
    public void testListJobs_EmptyResult() {
        when(jobMapper.selectWithPagination(0, 20))
                .thenReturn(Collections.emptyList());

        List<JobStatusDto> results = jobService.listJobs(0, 20);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ========== updateJobStatus ==========

    @Test
    public void testUpdateJobStatus_UpdatesBothDbAndMap() {
        CreateJobRequest request = TestDataFactory.createJobRequest(1);
        Long jobId = jobService.createJob(request);

        jobService.updateJobStatus(jobId, "PROCESSING");

        verify(jobMapper).updateStatus(jobId, "PROCESSING");
        JobStatusDto status = jobService.getJobStatus(jobId);
        assertEquals("PROCESSING", status.getStatus());
    }
}
