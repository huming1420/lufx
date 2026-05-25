package com.lufax.dashboard.model.entity;

public class ScenarioBaseline {
    private String period;
    private String metricVersion;
    private String scenario;
    private String scenarioLabel;
    private String inputsJson;

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getMetricVersion() { return metricVersion; }
    public void setMetricVersion(String metricVersion) { this.metricVersion = metricVersion; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public String getScenarioLabel() { return scenarioLabel; }
    public void setScenarioLabel(String scenarioLabel) { this.scenarioLabel = scenarioLabel; }
    public String getInputsJson() { return inputsJson; }
    public void setInputsJson(String inputsJson) { this.inputsJson = inputsJson; }
}
