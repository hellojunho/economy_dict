package com.economydict.service;

import com.economydict.dto.ImportTaskResponse;
import com.economydict.entity.ImportTask;
import com.economydict.entity.ImportTaskState;
import com.economydict.repository.ImportTaskRepository;
import java.io.PrintWriter;
import java.io.StringWriter;
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
        return response;
    }

    public String stackTrace(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
