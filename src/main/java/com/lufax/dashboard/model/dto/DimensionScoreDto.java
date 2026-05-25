package com.lufax.dashboard.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class DimensionScoreDto {

    private String dimension;

    private String label;

    private Double score;

    private String status;

    @JsonProperty("driver_metrics")
    private List<Map<String, Object>> driverMetrics;

    private Double contribution;

    private Long count;

    public DimensionScoreDto() {
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Map<String, Object>> getDriverMetrics() {
        return driverMetrics;
    }

    public void setDriverMetrics(List<Map<String, Object>> driverMetrics) {
        this.driverMetrics = driverMetrics;
    }

    public Double getContribution() {
        return contribution;
    }

    public void setContribution(Double contribution) {
        this.contribution = contribution;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}