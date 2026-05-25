package com.lufax.dashboard.service;

import com.lufax.dashboard.model.dto.JobStatusDto;
import com.lufax.dashboard.model.entity.InsightJob;
import com.lufax.dashboard.model.entity.InsightJobItem;
import com.lufax.dashboard.model.request.CreateJobRequest;
import com.lufax.dashboard.model.request.ScenarioInsightRequest;
import com.lufax.dashboard.repository.InsightJobItemMapper;
import com.lufax.dashboard.repository.InsightJobMapper;
import com.lufax.dashboard.util.JsonUtils;
import com.lufax.dashboard.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Service
public class InsightJobService {

    private static final Logger logger = LoggerFactory.getLogger(InsightJobService.class);
    private static final int LLM_CONCURRENCY = 2;

    private final Semaphore semaphore = new Semaphore(LLM_CONCURRENCY);
    private final Map<Long, JobStatusDto> jobStatusMap = new ConcurrentHashMap<>();

    @Autowired
    private InsightJobMapper jobMapper;

    @Autowired
    private InsightJobItemMapper jobItemMapper;

    @Autowired
    private InsightService insightService;

    @Autowired
    private JsonUtils jsonUtils;

    @Autowired
    private TimeUtils timeUtils;

    @Transactional
    public Long createJob(CreateJobRequest request) {
        InsightJob job = new InsightJob();
        job.setJobType(request.getJobType());
        job.setStatus("PENDING");
        job.setTotalItems(request.getItems() != null ? request.getItems().size() : 0);
        job.setCreatedAt(timeUtils.getCurrentTimeIso8601());
        job.setUpdatedAt(job.getCreatedAt());

        jobMapper.insert(job);

        if (request.getItems() != null) {
            for (CreateJobRequest.JobItemRequest item : request.getItems()) {
                InsightJobItem jobItem = new InsightJobItem();
                jobItem.setJobId(job.getId());
                jobItem.setScenario(item.getScenario());
                jobItem.setMetricDate(item.getMetricDate());
                jobItem.setInputData(item.getInputData());
                jobItem.setStatus("PENDING");
                jobItem.setCreatedAt(job.getCreatedAt());
                jobItemMapper.insert(jobItem);
            }
        }

        JobStatusDto status = new JobStatusDto();
        status.setJobId(job.getId().toString());
        status.setStatus("PENDING");
        status.setTotal(request.getItems() != null ? request.getItems().size() : 0);
        status.setCompleted(0);
        status.setFailed(0);
        jobStatusMap.put(job.getId(), status);

        return job.getId();
    }

    @Async("taskExecutor")
    public void processJobAsync(Long jobId) {
        try {
            logger.info("Starting job processing: {}", jobId);
            updateJobStatus(jobId, "PROCESSING");

            List<InsightJobItem> items = jobItemMapper.selectByJobId(jobId.toString());

            for (InsightJobItem item : items) {
                processJobItem(jobId, item);
            }

            updateJobStatus(jobId, "COMPLETED");
        } catch (Exception e) {
            logger.error("Error processing job: {}", jobId, e);
            updateJobStatus(jobId, "FAILED");
        }
    }

    private void processJobItem(Long jobId, InsightJobItem item) {
        try {
            semaphore.acquire();
            logger.info("Processing job item: {} for job: {}", item.getId(), jobId);

            ScenarioInsightRequest request = new ScenarioInsightRequest();
            request.setScenario(item.getScenario());
            request.setMetricDate(item.getMetricDate());

            if (item.getInputData() != null && !item.getInputData().isEmpty()) {
                Map<String, Object> inputData = jsonUtils.toMap(item.getInputData());
                request.setInputData(inputData);
            }

            insightService.generateScenarioInsight(request);

            item.setStatus("COMPLETED");
            item.setUpdatedAt(timeUtils.getCurrentTimeIso8601());
            jobItemMapper.update(item);

            updateJobProgress(jobId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Job item processing interrupted: {}", item.getId());
            item.setStatus("FAILED");
            item.setErrorMessage("Processing interrupted");
            jobItemMapper.update(item);
        } catch (Exception e) {
            logger.error("Error processing job item: {}", item.getId(), e);
            item.setStatus("FAILED");
            item.setErrorMessage(e.getMessage());
            jobItemMapper.update(item);
            updateJobProgress(jobId);
        } finally {
            semaphore.release();
        }
    }

    private void updateJobProgress(Long jobId) {
        JobStatusDto status = jobStatusMap.get(jobId);
        if (status != null) {
            status.setCompleted(status.getCompleted() + 1);
        }
    }

    public JobStatusDto getJobStatus(Long jobId) {
        JobStatusDto status = jobStatusMap.get(jobId);
        if (status != null) {
            return status;
        }

        InsightJob job = jobMapper.selectById(jobId.toString());
        if (job == null) {
            return null;
        }

        List<InsightJobItem> items = jobItemMapper.selectByJobId(jobId.toString());
        long completed = items.stream().filter(i -> "COMPLETED".equals(i.getStatus())).count();
        long failed = items.stream().filter(i -> "FAILED".equals(i.getStatus())).count();

        status = new JobStatusDto();
        status.setJobId(jobId.toString());
        status.setStatus(job.getStatus());
        status.setTotal(items.size());
        status.setCompleted((int) completed);
        status.setFailed((int) failed);

        jobStatusMap.put(jobId, status);
        return status;
    }

    public boolean cancelJob(Long jobId) {
        InsightJob job = jobMapper.selectById(jobId.toString());
        if (job == null) {
            return false;
        }

        if ("PENDING".equals(job.getStatus()) || "PROCESSING".equals(job.getStatus())) {
            jobMapper.updateStatus(jobId, "CANCELLED");
            JobStatusDto status = jobStatusMap.get(jobId);
            if (status != null) {
                status.setStatus("CANCELLED");
            }
            return true;
        }

        return false;
    }

    @Transactional
    public void updateJobStatus(Long jobId, String status) {
        jobMapper.updateStatus(jobId, status);

        JobStatusDto statusDto = jobStatusMap.get(jobId);
        if (statusDto != null) {
            statusDto.setStatus(status);
        }
    }

    public List<JobStatusDto> listJobs(int offset, int limit) {
        List<InsightJob> jobs = jobMapper.selectWithPagination(offset, limit);
        List<JobStatusDto> result = new ArrayList<>();

        for (InsightJob job : jobs) {
            JobStatusDto status = new JobStatusDto();
            status.setJobId(job.getId().toString());
            status.setStatus(job.getStatus());
            status.setPeriod(job.getPeriod());
            status.setCreatedAt(job.getCreatedAt());
            status.setStartedAt(job.getStartedAt());
            status.setFinishedAt(job.getFinishedAt());
            result.add(status);
        }

        return result;
    }

    public boolean deleteJob(Long jobId) {
        InsightJob job = jobMapper.selectById(jobId.toString());
        if (job == null) {
            return false;
        }

        if ("COMPLETED".equals(job.getStatus()) || "FAILED".equals(job.getStatus())) {
            jobItemMapper.deleteByJobId(jobId);
            jobMapper.deleteById(jobId);
            jobStatusMap.remove(jobId);
            return true;
        }

        return false;
    }
}