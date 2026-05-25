package com.lufax.dashboard.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OffsetFactorDto {

    private String factor;

    private String text;

    private List<String> evidence;

    @JsonProperty("total_factor")
    private Double totalFactor;

    @JsonProperty("day_of_week_factor")
    private Double dayOfWeekFactor;

    @JsonProperty("holiday_factor")
    private Double holidayFactor;

    @JsonProperty("trend_factor")
    private Double trendFactor;

    @JsonProperty("promotion_factor")
    private Double promotionFactor;

    public OffsetFactorDto() {
    }

    public String getFactor() {
        return factor;
    }

    public void setFactor(String factor) {
        this.factor = factor;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }

    public Double getTotalFactor() {
        return totalFactor;
    }

    public void setTotalFactor(Double totalFactor) {
        this.totalFactor = totalFactor;
    }

    public Double getDayOfWeekFactor() {
        return dayOfWeekFactor;
    }

    public void setDayOfWeekFactor(Double dayOfWeekFactor) {
        this.dayOfWeekFactor = dayOfWeekFactor;
    }

    public Double getHolidayFactor() {
        return holidayFactor;
    }

    public void setHolidayFactor(Double holidayFactor) {
        this.holidayFactor = holidayFactor;
    }

    public Double getTrendFactor() {
        return trendFactor;
    }

    public void setTrendFactor(Double trendFactor) {
        this.trendFactor = trendFactor;
    }

    public Double getPromotionFactor() {
        return promotionFactor;
    }

    public void setPromotionFactor(Double promotionFactor) {
        this.promotionFactor = promotionFactor;
    }
}