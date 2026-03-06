package com.economydict.service;

import com.economydict.dto.ImportTaskResponse;
import com.economydict.entity.ImportTask;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfImportJobService {
    private final JobLauncher jobLauncher;
    private final Job pdfImportJob;
    private final ImportTaskService taskService;

    public PdfImportJobService(JobLauncher jobLauncher,
                               Job pdfImportJob,
                               ImportTaskService taskService) {
        this.jobLauncher = jobLauncher;
        this.pdfImportJob = pdfImportJob;
        this.taskService = taskService;
    }

    public ImportTaskResponse submit(MultipartFile file) {
        ImportTask task = taskService.createTask();
        String filePath = storeTempFile(task.getTaskId(), file);
        runAsync(task.getTaskId(), filePath);
        return taskService.toResponse(task);
    }

    @Async
    public void runAsync(String taskId, String filePath) {
        JobParameters params = new JobParametersBuilder()
                .addString("taskId", taskId)
                .addString("filePath", filePath)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        try {
            jobLauncher.run(pdfImportJob, params);
        } catch (Exception ex) {
            taskService.markFailed(taskId, taskService.stackTrace(ex));
        }
    }

    private String storeTempFile(String taskId, MultipartFile file) {
        try {
            Path tempFile = Files.createTempFile("pdf-import-" + taskId + "-", ".pdf");
            file.transferTo(tempFile.toFile());
            return tempFile.toAbsolutePath().toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to store PDF", ex);
        }
    }
}
