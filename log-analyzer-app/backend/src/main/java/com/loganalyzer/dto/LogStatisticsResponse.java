package com.loganalyzer.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class LogStatisticsResponse {
    private long totalLogs;
    private Map<String, Long> logLevelCounts;
    private long recentLogsCount;
    private LocalDateTime lastUpdated;
    private long errorCount;
    private long warningCount;
    private long infoCount;
    private long debugCount;
    private long traceCount;
    private long fatalCount;

    public LogStatisticsResponse() {
        this.lastUpdated = LocalDateTime.now();
    }

    public long getTotalLogs() {
        return totalLogs;
    }

    public void setTotalLogs(long totalLogs) {
        this.totalLogs = totalLogs;
    }

    public Map<String, Long> getLogLevelCounts() {
        return logLevelCounts;
    }

    public void setLogLevelCounts(Map<String, Long> logLevelCounts) {
        this.logLevelCounts = logLevelCounts;
    }

    public long getRecentLogsCount() {
        return recentLogsCount;
    }

    public void setRecentLogsCount(long recentLogsCount) {
        this.recentLogsCount = recentLogsCount;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public long getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(long warningCount) {
        this.warningCount = warningCount;
    }

    public long getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(long infoCount) {
        this.infoCount = infoCount;
    }

    public long getDebugCount() {
        return debugCount;
    }

    public void setDebugCount(long debugCount) {
        this.debugCount = debugCount;
    }

    public long getTraceCount() {
        return traceCount;
    }

    public void setTraceCount(long traceCount) {
        this.traceCount = traceCount;
    }

    public long getFatalCount() {
        return fatalCount;
    }

    public void setFatalCount(long fatalCount) {
        this.fatalCount = fatalCount;
    }
}