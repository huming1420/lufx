package com.lufax.dashboard.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lufax.dashboard.model.dto.CardInsightDto;
import com.lufax.dashboard.model.dto.JobStatusDto;
import java.util.List;

public class PageStateResponse {

    private String period;

    @JsonProperty("metric_version")
    private String metricVersion;

    @JsonProperty("prompt_version")
    private String promptVersion;

    private List<CardInsightDto> cards;

    private CardInsightDto dashboard;

    @JsonProperty("active_jobs")
    private List<JobStatusDto> activeJobs;

    public PageStateResponse() {
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

    public List<CardInsightDto> getCards() {
        return cards;
    }

    public void setCards(List<CardInsightDto> cards) {
        this.cards = cards;
    }

    public CardInsightDto getDashboard() {
        return dashboard;
    }

    public void setDashboard(CardInsightDto dashboard) {
        this.dashboard = dashboard;
    }

    public List<JobStatusDto> getActiveJobs() {
        return activeJobs;
    }

    public void setActiveJobs(List<JobStatusDto> activeJobs) {
        this.activeJobs = activeJobs;
    }
}
