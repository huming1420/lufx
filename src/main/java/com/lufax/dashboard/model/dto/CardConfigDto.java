package com.lufax.dashboard.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class CardConfigDto {

    @JsonProperty("card_id")
    private String cardId;

    private String title;

    private String module;

    @JsonProperty("metric_codes")
    private List<String> metricCodes;

    @JsonProperty("default_prompt_version")
    private String defaultPromptVersion;

    @JsonProperty("card_name")
    private String cardName;

    private String description;

    private Double threshold;

    private Boolean enabled;

    public CardConfigDto() {
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public List<String> getMetricCodes() {
        return metricCodes;
    }

    public void setMetricCodes(List<String> metricCodes) {
        this.metricCodes = metricCodes;
    }

    public String getDefaultPromptVersion() {
        return defaultPromptVersion;
    }

    public void setDefaultPromptVersion(String defaultPromptVersion) {
        this.defaultPromptVersion = defaultPromptVersion;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
