package com.lufax.dashboard.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RootCauseDto {

    @JsonProperty("metric_code")
    private String metricCode;

    @JsonProperty("metric_name")
    private String metricName;

    private String segment;

    private Double value;

    private String unit;

    private Double yoy;

    private Double mom;

    private Double budget;

    @JsonProperty("budget_gap")
    private Double budgetGap;

    private String status;

    private String dimension;

    @JsonProperty("impact_score")
    private Double impactScore;

    private String reason;

    private Integer rank;

    private String description;

    private Double impact;

    private Double confidence;

    public RootCauseDto() {
    }

    public String getMetricCode() {
        return metricCode;
    }

    public void setMetricCode(String metricCode) {
        this.metricCode = metricCode;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public void setValue(String value) {
        if (value != null) {
            try {
                this.value = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                this.value = null;
            }
        } else {
            this.value = null;
        }
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getYoy() {
        return yoy;
    }

    public void setYoy(Double yoy) {
        this.yoy = yoy;
    }

    public Double getMom() {
        return mom;
    }

    public void setMom(Double mom) {
        this.mom = mom;
    }

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public Double getBudgetGap() {
        return budgetGap;
    }

    public void setBudgetGap(Double budgetGap) {
        this.budgetGap = budgetGap;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public Double getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(Double impactScore) {
        this.impactScore = impactScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getImpact() {
        return impact;
    }

    public void setImpact(Double impact) {
        this.impact = impact;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}