package com.lufax.dashboard.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class DashboardInsightRequest {

    private String period;

    private String scenario;

    @JsonProperty("input_data")
    private Map<String, Object> inputData;

    @JsonProperty("metric_date")
    private String metricDate;

    @JsonProperty("report_type")
    private String reportType;

    public DashboardInsightRequest() {
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public Map<String, Object> getInputData() {
        return inputData;
    }

    public void setInputData(Map<String, Object> inputData) {
        this.inputData = inputData;
    }

    public String getMetricDate() {
        return metricDate;
    }

    public void setMetricDate(String metricDate) {
        this.metricDate = metricDate;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
}