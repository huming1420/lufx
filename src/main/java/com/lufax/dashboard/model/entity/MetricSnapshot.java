package com.lufax.dashboard.model.entity;

import java.sql.Timestamp;

public class MetricSnapshot {
    private Long id;
    private String period;
    private String metricVersion;
    private String sourceType;
    private String sourceLabel;
    private String sourceFile;
    private Timestamp importedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getMetricVersion() { return metricVersion; }
    public void setMetricVersion(String metricVersion) { this.metricVersion = metricVersion; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceLabel() { return sourceLabel; }
    public void setSourceLabel(String sourceLabel) { this.sourceLabel = sourceLabel; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    public Timestamp getImportedAt() { return importedAt; }
    public void setImportedAt(Timestamp importedAt) { this.importedAt = importedAt; }
}
