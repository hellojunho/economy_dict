package com.economydict.dto;

import com.economydict.entity.ImportTaskState;
import java.time.Instant;

public class ImportTaskResponse {
    private String taskId;
    private ImportTaskState state;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant failedAt;
    private String errorLog;
    private String originalFileName;
    private Integer totalUnits;
    private Integer processedUnits;
    private Double progressPercent;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public ImportTaskState getState() {
        return state;
    }

    public void setState(ImportTaskState state) {
        this.state = state;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public Integer getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(Integer totalUnits) {
        this.totalUnits = totalUnits;
    }

    public Integer getProcessedUnits() {
        return processedUnits;
    }

    public void setProcessedUnits(Integer processedUnits) {
        this.processedUnits = processedUnits;
    }

    public Double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Double progressPercent) {
        this.progressPercent = progressPercent;
    }
}
