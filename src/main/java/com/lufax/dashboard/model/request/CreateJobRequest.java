package com.lufax.dashboard.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CreateJobRequest {

    @JsonProperty("job_type")
    private String jobType;

    private String period;

    private String scope;

    @JsonProperty("card_ids")
    private List<String> cardIds;

    @JsonProperty("force_refresh")
    private Boolean forceRefresh;

    private List<JobItemRequest> items;

    public CreateJobRequest() {
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
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

    public List<String> getCardIds() {
        return cardIds;
    }

    public void setCardIds(List<String> cardIds) {
        this.cardIds = cardIds;
    }

    public Boolean getForceRefresh() {
        return forceRefresh;
    }

    public void setForceRefresh(Boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    public List<JobItemRequest> getItems() {
        return items;
    }

    public void setItems(List<JobItemRequest> items) {
        this.items = items;
    }

    public static class JobItemRequest {
        private String scenario;
        @JsonProperty("metric_date")
        private String metricDate;
        @JsonProperty("input_data")
        private String inputData;

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
    }
}