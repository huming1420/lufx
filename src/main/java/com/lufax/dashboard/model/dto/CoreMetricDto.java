package com.lufax.dashboard.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CoreMetricDto {

    @JsonProperty("metric_code")
    private String metricCode;

    @JsonProperty("metric_name")
    private String metricName;

    private Double value;

    @JsonProperty("display_value")
    private String displayValue;

    private String unit;

    private Double yoy;

    @JsonProperty("display_yoy")
    private String displayYoy;

    private Double mom;

    private Double budget;

    @JsonProperty("budget_gap")
    private Double budgetGap;

    @JsonProperty("display_budget_gap")
    private String displayBudgetGap;

    private Double redline;

    private Double yellowline;

    @JsonProperty("distance_to_redline")
    private Double distanceToRedline;

    @JsonProperty("display_distance_to_redline")
    private String displayDistanceToRedline;

    private String status;

    private String direction;

    private String module;

    private Double score;

    @JsonProperty("offset_factor")
    private Double offsetFactor;

    @JsonProperty("total_amount")
    private Double totalAmount;

    private Long count;

    @JsonProperty("avg_amount")
    private Double avgAmount;

    @JsonProperty("metric_date")
    private String metricDate;

    @JsonProperty("report_type")
    private String reportType;

    public CoreMetricDto() {
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

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
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

    public String getDisplayYoy() {
        return displayYoy;
    }

    public void setDisplayYoy(String displayYoy) {
        this.displayYoy = displayYoy;
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

    public String getDisplayBudgetGap() {
        return displayBudgetGap;
    }

    public void setDisplayBudgetGap(String displayBudgetGap) {
        this.displayBudgetGap = displayBudgetGap;
    }

    public Double getRedline() {
        return redline;
    }

    public void setRedline(Double redline) {
        this.redline = redline;
    }

    public Double getYellowline() {
        return yellowline;
    }

    public void setYellowline(Double yellowline) {
        this.yellowline = yellowline;
    }

    public Double getDistanceToRedline() {
        return distanceToRedline;
    }

    public void setDistanceToRedline(Double distanceToRedline) {
        this.distanceToRedline = distanceToRedline;
    }

    public String getDisplayDistanceToRedline() {
        return displayDistanceToRedline;
    }

    public void setDisplayDistanceToRedline(String displayDistanceToRedline) {
        this.displayDistanceToRedline = displayDistanceToRedline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
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

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getAvgAmount() {
        return avgAmount;
    }

    public void setAvgAmount(Double avgAmount) {
        this.avgAmount = avgAmount;
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