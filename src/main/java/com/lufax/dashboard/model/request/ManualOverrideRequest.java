package com.lufax.dashboard.model.request;

import java.util.Map;

public class ManualOverrideRequest {

    private String cardId;

    private Map<String, Object> payload;

    private String overrideValue;

    private String reason;

    public ManualOverrideRequest() {
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public String getOverrideValue() {
        return overrideValue;
    }

    public void setOverrideValue(String overrideValue) {
        this.overrideValue = overrideValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
