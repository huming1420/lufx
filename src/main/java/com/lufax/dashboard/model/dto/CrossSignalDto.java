package com.lufax.dashboard.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CrossSignalDto {

    private String type;

    private String signal;

    @JsonProperty("supporting_facts")
    private List<String> supportingFacts;

    @JsonProperty("analysis_hint")
    private String analysisHint;

    @JsonProperty("signal_name")
    private String signalName;

    private Double score;

    private String trend;

    private String description;

    public CrossSignalDto() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
    }

    public List<String> getSupportingFacts() {
        return supportingFacts;
    }

    public void setSupportingFacts(List<String> supportingFacts) {
        this.supportingFacts = supportingFacts;
    }

    public String getAnalysisHint() {
        return analysisHint;
    }

    public void setAnalysisHint(String analysisHint) {
        this.analysisHint = analysisHint;
    }

    public String getSignalName() {
        return signalName;
    }

    public void setSignalName(String signalName) {
        this.signalName = signalName;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}