package com.economydict.service;

import com.economydict.dto.ImportTaskResponse;
import com.economydict.entity.ImportTask;
import com.economydict.entity.ImportTaskState;
import com.economydict.repository.ImportTaskRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ImportTaskService {
    private final ImportTaskRepository importTaskRepository;

    public ImportTaskService(ImportTaskRepository importTaskRepository) {
        this.importTaskRepository = importTaskRepository;
    }

    public ImportTask createTask() {
        ImportTask task = new ImportTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setState(ImportTaskState.READY);
        task.setCreatedAt(Instant.now());
        return importTaskRepository.save(task);
    }

    public ImportTask markStarted(String taskId) {
        ImportTask task = getTask(taskId);
        task.setState(ImportTaskState.STARTED);
        task.setStartedAt(Instant.now());
        return importTaskRepository.save(task);
    }

    public ImportTask markPending(String taskId) {
        ImportTask task = getTask(taskId);
        task.setState(ImportTaskState.PENDING);
        return importTaskRepository.save(task);
    }

    public ImportTask markFinished(String taskId) {
        ImportTask task = getTask(taskId);
        task.setState(ImportTaskState.FINISHED);
        task.setFinishedAt(Instant.now());
        return importTaskRepository.save(task);
    }

    public ImportTask updateProgress(String taskId, int processedUnits, int totalUnits) {
        ImportTask task = getTask(taskId);
        task.setProcessedUnits(processedUnits);
        if (totalUnits > 0) {
            task.setTotalUnits(totalUnits);
        }
        if (task.getState() == ImportTaskState.READY || task.getState() == ImportTaskState.STARTED) {
            task.setState(ImportTaskState.PENDING);
        }
        return importTaskRepository.save(task);
    }

    public ImportTask markFailed(String taskId, String errorLog) {
        ImportTask task = getTask(taskId);
        task.setState(ImportTaskState.FAILED);
        task.setFailedAt(Instant.now());
        task.setErrorLog(errorLog);
        return importTaskRepository.save(task);
    }

    public ImportTask getTask(String taskId) {
        return importTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    }

    public ImportTaskResponse toResponse(ImportTask task) {
        ImportTaskResponse response = new ImportTaskResponse();
        response.setTaskId(task.getTaskId());
        response.setState(task.getState());
        response.setCreatedAt(task.getCreatedAt());
        response.setStartedAt(task.getStartedAt());
        response.setFinishedAt(task.getFinishedAt());
        response.setFailedAt(task.getFailedAt());
        response.setErrorLog(task.getErrorLog());
        response.setProcessedUnits(task.getProcessedUnits());
        response.setTotalUnits(task.getTotalUnits());
        if (task.getTotalUnits() != null && task.getTotalUnits() > 0 && task.getProcessedUnits() != null) {
            double percent = (task.getProcessedUnits() * 100.0) / task.getTotalUnits();
            response.setProgressPercent(Math.min(100.0, percent));
        } else {
            response.setProgressPercent(0.0);
        }
        return response;
    }

    public String stackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
