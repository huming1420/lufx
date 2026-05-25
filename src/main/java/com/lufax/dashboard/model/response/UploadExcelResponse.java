package com.lufax.dashboard.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadExcelResponse {

    private String status;

    private String period;

    @JsonProperty("insight_job_id")
    private String insightJobId;

    private String message;

    private Boolean success;

    private String filename;

    @JsonProperty("rows_processed")
    private Integer rowsProcessed;

    public UploadExcelResponse() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getInsightJobId() {
        return insightJobId;
    }

    public void setInsightJobId(String insightJobId) {
        this.insightJobId = insightJobId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getSuccess() {
        return success;
    }

    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getRowsProcessed() {
        return rowsProcessed;
    }

    public void setRowsProcessed(Integer rowsProcessed) {
        this.rowsProcessed = rowsProcessed;
    }

    public void setRowsProcessed(int rowsProcessed) {
        this.rowsProcessed = rowsProcessed;
    }
}
