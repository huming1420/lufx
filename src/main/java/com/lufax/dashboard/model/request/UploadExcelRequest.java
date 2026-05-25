package com.lufax.dashboard.model.request;

import java.util.List;
import java.util.Map;

public class UploadExcelRequest {

    private String period;

    private String scenario;

    private List<Map<String, Object>> rows;

    public UploadExcelRequest() {
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

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }
}