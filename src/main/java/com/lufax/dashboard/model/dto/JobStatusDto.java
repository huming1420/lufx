package com.lufax.dashboard.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lufax.dashboard.model.entity.InsightJobItem;
import java.util.List;

public class JobStatusDto {

    @JsonProperty("job_id")
    private String jobId;

    private String period;

    @JsonProperty("metric_version")
    private String metricVersion;

    @JsonProperty("prompt_version")
    private String promptVersion;

    @JsonProperty("job_type")
    private String jobType;

    private String status;

    @JsonProperty("total_count")
    private Integer totalCount;

    @JsonProperty("finished_count")
    private Integer finishedCount;

    @JsonProperty("failed_count")
    private Integer failedCount;

    @JsonProperty("force_refresh")
    private Boolean forceRefresh;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("started_at")
    private String startedAt;

    @JsonProperty("finished_at")
    private String finishedAt;

    @JsonProperty("error_message")
    private String errorMessage;

    private List<InsightJobItem> items;

    public JobStatusDto() {
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId != null ? jobId.toString() : null;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getMetricVersion() {
        return metricVersion;
    }

    public void setMetricVersion(String metricVersion) {
        this.metricVersion = metricVersion;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getFinishedCount() {
        return finishedCount;
    }

    public void setFinishedCount(Integer finishedCount) {
        this.finishedCount = finishedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public Boolean getForceRefresh() {
        return forceRefresh;
    }

    public void setForceRefresh(Boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<InsightJobItem> getItems() {
        return items;
    }

    public void setItems(List<InsightJobItem> items) {
        this.items = items;
    }

    public void setTotal(int total) {
        this.totalCount = total;
    }

    public void setCompleted(int completed) {
        this.finishedCount = completed;
    }

    public void setFailed(int failed) {
        this.failedCount = failed;
    }

    public int getCompleted() {
        return finishedCount != null ? finishedCount : 0;
    }

    public int getTotal() {
        return totalCount != null ? totalCount : 0;
    }

    public int getFailed() {
        return failedCount != null ? failedCount : 0;
    }
}
