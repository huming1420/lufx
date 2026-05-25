package com.lufax.dashboard.model.response;

import java.util.Map;

public class ManualOverrideResponse {

    private String cardId;

    private String status;

    private Boolean accepted;

    private Boolean success;

    private String message;

    private Map<String, Object> payload;

    private Long overrideId;

    public ManualOverrideResponse() {
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Boolean getSuccess() {
        return success;
    }

    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Long getOverrideId() {
        return overrideId;
    }

    public void setOverrideId(Long overrideId) {
        this.overrideId = overrideId;
    }

    public void setOverrideId(long overrideId) {
        this.overrideId = overrideId;
    }
}
