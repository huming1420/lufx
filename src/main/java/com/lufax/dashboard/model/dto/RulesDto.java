package com.lufax.dashboard.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RulesDto {

    @JsonProperty("health_score")
    private Double healthScore;

    @JsonProperty("dimension_scores")
    private List<DimensionScoreDto> dimensionScores;

    @JsonProperty("root_causes")
    private List<RootCauseDto> rootCauses;

    @JsonProperty("offset_factors")
    private List<OffsetFactorDto> offsetFactors;

    @JsonProperty("cross_signals")
    private List<CrossSignalDto> crossSignals;

    private List<Object> rules;

    @JsonProperty("score_threshold")
    private Double scoreThreshold;

    private String version;

    public RulesDto() {
    }

    public Double getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(Double healthScore) {
        this.healthScore = healthScore;
    }

    public List<DimensionScoreDto> getDimensionScores() {
        return dimensionScores;
    }

    public void setDimensionScores(List<DimensionScoreDto> dimensionScores) {
        this.dimensionScores = dimensionScores;
    }

    public List<RootCauseDto> getRootCauses() {
        return rootCauses;
    }

    public void setRootCauses(List<RootCauseDto> rootCauses) {
        this.rootCauses = rootCauses;
    }

    public List<OffsetFactorDto> getOffsetFactors() {
        return offsetFactors;
    }

    public void setOffsetFactors(List<OffsetFactorDto> offsetFactors) {
        this.offsetFactors = offsetFactors;
    }

    public List<CrossSignalDto> getCrossSignals() {
        return crossSignals;
    }

    public void setCrossSignals(List<CrossSignalDto> crossSignals) {
        this.crossSignals = crossSignals;
    }

    public List<Object> getRules() {
        return rules;
    }

    public void setRules(List<Object> rules) {
        this.rules = rules;
    }

    public Double getScoreThreshold() {
        return scoreThreshold;
    }

    public void setScoreThreshold(Double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}