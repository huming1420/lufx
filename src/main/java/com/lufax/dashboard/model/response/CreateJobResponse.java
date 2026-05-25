package com.lufax.dashboard.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateJobResponse {

    @JsonProperty("job_id")
    private String jobId;

    private String period;

    private String scope;

    private String status;

    private String message;

    public CreateJobResponse() {
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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}