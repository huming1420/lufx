package com.lufax.dashboard.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class ScenarioInsightRequest {

    @JsonProperty("scenario_id")
    private String scenarioId;

    private String period;

    private Map<String, Object> inputs;

    private Map<String, Object> computed;

    private Map<String, Object> sensitivity;

    @JsonProperty("cache_only")
    private Boolean cacheOnly;

    private String scenario;

    @JsonProperty("metric_date")
    private String metricDate;

    @JsonProperty("input_data")
    private Map<String, Object> inputData;

    public ScenarioInsightRequest() {
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public Map<String, Object> getComputed() {
        return computed;
    }

    public void setComputed(Map<String, Object> computed) {
        this.computed = computed;
    }

    public Map<String, Object> getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(Map<String, Object> sensitivity) {
        this.sensitivity = sensitivity;
    }

    public Boolean getCacheOnly() {
        return cacheOnly;
    }

    public void setCacheOnly(Boolean cacheOnly) {
        this.cacheOnly = cacheOnly;
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

    public Map<String, Object> getInputData() {
        return inputData;
    }

    public void setInputData(Map<String, Object> inputData) {
        this.inputData = inputData;
    }
}