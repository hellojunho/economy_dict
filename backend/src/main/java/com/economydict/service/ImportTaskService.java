package com.economydict.service;

import com.economydict.dto.ImportTaskResponse;
import com.economydict.dto.WordUploadStatusResponse;
import com.economydict.entity.ImportTask;
import com.economydict.entity.ImportTaskState;
import com.economydict.repository.ImportTaskRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ImportTaskService {
    private final ImportTaskRepository importTaskRepository;
    private final ErrorLogService errorLogService;

    public ImportTaskService(ImportTaskRepository importTaskRepository, ErrorLogService errorLogService) {
        this.importTaskRepository = importTaskRepository;
        this.errorLogService = errorLogService;
    }

    public ImportTask createTask() {
        ImportTask task = new ImportTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setState(ImportTaskState.READY);
        task.setCreatedAt(Instant.now());
        task.setProcessedUnits(0);
        task.setTotalUnits(0);
        task.setRequestedByUserId(resolveCurrentUserId());
        return importTaskRepository.save(task);
    }

    public ImportTask createTask(String originalFileName, String fileType) {
        ImportTask task = createTask();
        task.setOriginalFileName(originalFileName);
        task.setFileType(fileType);
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
        if (task.getTotalUnits() != null && task.getTotalUnits() > 0) {
            task.setProcessedUnits(task.getTotalUnits());
        }
        return importTaskRepository.save(task);
    }

    public ImportTask updateTotalUnits(String taskId, int totalUnits) {
        ImportTask task = getTask(taskId);
        task.setTotalUnits(Math.max(0, totalUnits));
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
        String logFile = errorLogService.writeBackgroundError(
                UUID.randomUUID().toString(),
                "IMPORT_TASK_FAILED",
                "The import task failed during asynchronous processing.",
                List.of(
                        "taskId=" + task.getTaskId(),
                        "originalFileName=" + safeValue(task.getOriginalFileName()),
                        "fileType=" + safeValue(task.getFileType())
                ),
                task.getRequestedByUserId(),
                errorLog
        );
        task.setErrorLog(logFile == null ? errorLog : "logFile=" + logFile + "\n" + errorLog);
        return importTaskRepository.save(task);
    }

    public ImportTask getTask(String taskId) {
        return importTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    }

    public List<WordUploadStatusResponse> listRecentTasks() {
        return importTaskRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toWordUploadStatus)
                .collect(Collectors.toList());
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
        response.setOriginalFileName(task.getOriginalFileName());
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

    public WordUploadStatusResponse toWordUploadStatus(ImportTask task) {
        WordUploadStatusResponse response = new WordUploadStatusResponse();
        response.setFileId(task.getTaskId());
        response.setOriginalFileName(task.getOriginalFileName());
        response.setStatus(task.getState().name());
        response.setProgressPercent(task.getTotalUnits() != null && task.getTotalUnits() > 0 && task.getProcessedUnits() != null
                ? Math.min(100.0, task.getProcessedUnits() * 100.0 / task.getTotalUnits())
                : 0.0);
        response.setEstimatedTime(response.getProgressPercent() >= 100.0 ? "0m" : "Calculating");
        response.setErrorLog(task.getErrorLog());
        switch (task.getState()) {
            case READY -> response.setMessage("파일 업로드가 접수되었습니다.");
            case STARTED, PENDING -> response.setMessage("파일 분석이 진행 중입니다.");
            case FINISHED -> response.setMessage("파일 분석이 완료되었습니다.");
            case FAILED -> response.setMessage("파일 분석이 실패했습니다.");
            default -> response.setMessage("상태를 확인하세요.");
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

    private String resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return "anonymous";
        }
        return authentication.getName();
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
