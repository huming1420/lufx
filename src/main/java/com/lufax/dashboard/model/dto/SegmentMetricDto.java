package com.lufax.dashboard.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SegmentMetricDto {

    @JsonProperty("metric_code")
    private String metricCode;

    @JsonProperty("metric_name")
    private String metricName;

    private String dimension;

    private String segment;

    private Double value;

    @JsonProperty("display_value")
    private String displayValue;

    private String unit;

    private Double yoy;

    @JsonProperty("display_yoy")
    private String displayYoy;

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

    @JsonProperty("product_type")
    private String productType;

    @JsonProperty("channel_type")
    private String channelType;

    @JsonProperty("customer_type")
    private String customerType;

    private String vintage;

    private String mob;

    @JsonProperty("segment_name")
    private String segmentName;

    private Double amount;

    private Double score;

    private Long count;

    @JsonProperty("conversion_rate")
    private Double conversionRate;

    public SegmentMetricDto() {
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

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
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

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public String getVintage() {
        return vintage;
    }

    public void setVintage(String vintage) {
        this.vintage = vintage;
    }

    public String getMob() {
        return mob;
    }

    public void setMob(String mob) {
        this.mob = mob;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(Double conversionRate) {
        this.conversionRate = conversionRate;
    }
}