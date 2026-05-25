package com.lufax.dashboard.controller;

import com.lufax.dashboard.model.dto.JobStatusDto;
import com.lufax.dashboard.model.entity.InsightJob;
import com.lufax.dashboard.model.request.CreateJobRequest;
import com.lufax.dashboard.model.response.CreateJobResponse;
import com.lufax.dashboard.service.InsightJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job")
public class JobController {

    private static final Logger logger = LoggerFactory.getLogger(JobController.class);

    @Autowired
    private InsightJobService jobService;

    @PostMapping("/create")
    public ResponseEntity<CreateJobResponse> createJob(@RequestBody CreateJobRequest request) {
        logger.info("Received create job request with {} items", request.getItems() != null ? request.getItems().size() : 0);
        Long jobId = jobService.createJob(request);

        CreateJobResponse response = new CreateJobResponse();
        response.setJobId(jobId);
        response.setMessage("Job created successfully");

        jobService.processJobAsync(jobId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{job_id}")
    public ResponseEntity<JobStatusDto> getJobStatus(@PathVariable Long job_id) {
        logger.info("Received job status request for job_id: {}", job_id);
        JobStatusDto status = jobService.getJobStatus(job_id);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @PostMapping("/cancel/{job_id}")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable Long job_id) {
        logger.info("Received cancel job request for job_id: {}", job_id);
        boolean cancelled = jobService.cancelJob(job_id);

        if (cancelled) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Job cancelled"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot cancel job"));
        }
    }

    @DeleteMapping("/{job_id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long job_id) {
        logger.info("Received delete job request for job_id: {}", job_id);
        jobService.deleteJob(job_id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<JobStatusDto>> listJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("Received list jobs request page: {}, size: {}", page, size);
        List<JobStatusDto> jobs = jobService.listJobs(page, size);
        return ResponseEntity.ok(jobs);
    }
}