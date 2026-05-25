package com.lufax.dashboard.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InsightResult {

    private Long id;

    private String scenario;

    @JsonProperty("metric_date")
    private String metricDate;

    @JsonProperty("scenario_hash")
    private String scenarioHash;

    @JsonProperty("insight_json")
    private String insightJson;

    private Double score;

    @JsonProperty("offset_factor")
    private Double offsetFactor;

    private String version;

    private String period;

    @JsonProperty("metric_version")
    private String metricVersion;

    @JsonProperty("prompt_version")
    private String promptVersion;

    @JsonProperty("insight_type")
    private String insightType;

    @JsonProperty("card_id")
    private String cardId;

    private String status;

    @JsonProperty("raw_llm_output")
    private String rawLlmOutput;

    @JsonProperty("raw_llm_output_json")
    private String rawLlmOutputJson;

    @JsonProperty("raw_fact_pack_json")
    private String rawFactPackJson;

    @JsonProperty("schema_name")
    private String schemaName;

    @JsonProperty("traffic_light")
    private String trafficLight;

    @JsonProperty("analysis_labels_json")
    private String analysisLabelsJson;

    @JsonProperty("result_json")
    private String resultJson;

    @JsonProperty("error_message")
    private String errorMessage;

    private Boolean validated;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public InsightResult() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getScenarioHash() {
        return scenarioHash;
    }

    public void setScenarioHash(String scenarioHash) {
        this.scenarioHash = scenarioHash;
    }

    public String getInsightJson() {
        return insightJson;
    }

    public void setInsightJson(String insightJson) {
        this.insightJson = insightJson;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getOffsetFactor() {
        return offsetFactor;
    }

    public void setOffsetFactor(Double offsetFactor) {
        this.offsetFactor = offsetFactor;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRawLlmOutput() {
        return rawLlmOutput;
    }

    public void setRawLlmOutput(String rawLlmOutput) {
        this.rawLlmOutput = rawLlmOutput;
    }

    public String getRawLlmOutputJson() {
        return rawLlmOutputJson;
    }

    public void setRawLlmOutputJson(String rawLlmOutputJson) {
        this.rawLlmOutputJson = rawLlmOutputJson;
    }

    public String getRawFactPackJson() {
        return rawFactPackJson;
    }

    public void setRawFactPackJson(String rawFactPackJson) {
        this.rawFactPackJson = rawFactPackJson;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTrafficLight() {
        return trafficLight;
    }

    public void setTrafficLight(String trafficLight) {
        this.trafficLight = trafficLight;
    }

    public String getAnalysisLabelsJson() {
        return analysisLabelsJson;
    }

    public void setAnalysisLabelsJson(String analysisLabelsJson) {
        this.analysisLabelsJson = analysisLabelsJson;
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

    public Boolean getValidated() {
        return validated;
    }

    public void setValidated(Boolean validated) {
        this.validated = validated;
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
