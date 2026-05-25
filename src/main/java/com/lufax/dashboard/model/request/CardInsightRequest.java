package com.lufax.dashboard.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class CardInsightRequest {

    @JsonProperty("card_id")
    private String cardId;

    private String period;

    private String scenario;

    @JsonProperty("input_data")
    private Map<String, Object> inputData;

    public CardInsightRequest() {
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public Map<String, Object> getInputData() {
        return inputData;
    }

    public void setInputData(Map<String, Object> inputData) {
        this.inputData = inputData;
    }
}