package com.lufax.dashboard.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InsightJobItem {

    private Integer id;

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("insight_type")
    private String insightType;

    @JsonProperty("card_id")
    private String cardId;

    private String scenario;

    @JsonProperty("metric_date")
    private String metricDate;

    @JsonProperty("input_data")
    private String inputData;

    private String status;

    @JsonProperty("result_id")
    private Integer resultId;

    @JsonProperty("result_json")
    private String resultJson;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public InsightJobItem() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getInsightType() {
        return insightType;
    }

    public void setInsightType(String insightType) {
        this.insightType = insightType;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getMetricDate() {
        return metricDate;
    }

    public void setMetricDate(String metricDate) {
        this.metricDate = metricDate;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getResultId() {
        return resultId;
    }

    public void setResultId(Integer resultId) {
        this.resultId = resultId;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}