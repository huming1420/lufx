package com.lufax.dashboard.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lufax.dashboard.model.dto.CardConfigDto;
import com.lufax.dashboard.model.dto.CoreMetricDto;
import com.lufax.dashboard.model.dto.RulesDto;
import com.lufax.dashboard.model.dto.ScenarioDefaultDto;
import com.lufax.dashboard.model.dto.SegmentMetricDto;
import com.lufax.dashboard.model.entity.InsightResult;
import java.util.List;
import java.util.Map;

public class DashboardDataResponse {

    private String period;

    @JsonProperty("metric_version")
    private String metricVersion;

    @JsonProperty("data_source")
    private Map<String, Object> dataSource;

    @JsonProperty("core_metrics")
    private List<CoreMetricDto> coreMetrics;

    @JsonProperty("segment_metrics")
    private List<SegmentMetricDto> segmentMetrics;

    @JsonProperty("scenario_defaults")
    private List<ScenarioDefaultDto> scenarioDefaults;

    private RulesDto rules;

    private List<CardConfigDto> cards;

    @JsonProperty("fact_pack_rules")
    private RulesDto factPackRules;

    private String scenario;

    @JsonProperty("metric_date")
    private String metricDate;

    private List<InsightResult> results;

    public DashboardDataResponse() {
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

    public Map<String, Object> getDataSource() {
        return dataSource;
    }

    public void setDataSource(Map<String, Object> dataSource) {
        this.dataSource = dataSource;
    }

    public List<CoreMetricDto> getCoreMetrics() {
        return coreMetrics;
    }

    public void setCoreMetrics(List<CoreMetricDto> coreMetrics) {
        this.coreMetrics = coreMetrics;
    }

    public List<SegmentMetricDto> getSegmentMetrics() {
        return segmentMetrics;
    }

    public void setSegmentMetrics(List<SegmentMetricDto> segmentMetrics) {
        this.segmentMetrics = segmentMetrics;
    }

    public List<ScenarioDefaultDto> getScenarioDefaults() {
        return scenarioDefaults;
    }

    public void setScenarioDefaults(List<ScenarioDefaultDto> scenarioDefaults) {
        this.scenarioDefaults = scenarioDefaults;
    }

    public RulesDto getRules() {
        return rules;
    }

    public void setRules(RulesDto rules) {
        this.rules = rules;
    }

    public List<CardConfigDto> getCards() {
        return cards;
    }

    public void setCards(List<CardConfigDto> cards) {
        this.cards = cards;
    }

    public RulesDto getFactPackRules() {
        return factPackRules;
    }

    public void setFactPackRules(RulesDto factPackRules) {
        this.factPackRules = factPackRules;
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

    public List<InsightResult> getResults() {
        return results;
    }

    public void setResults(List<InsightResult> results) {
        this.results = results;
    }
}
