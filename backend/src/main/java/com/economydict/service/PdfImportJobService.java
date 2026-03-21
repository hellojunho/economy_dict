package com.economydict.service;

import com.economydict.dto.WordUploadStatusResponse;
import com.economydict.batch.ImportChunk;
import com.economydict.entity.ImportTask;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    private final ImportContentExtractor importContentExtractor;

    public PdfImportJobService(JobLauncher jobLauncher,
                               Job pdfImportJob,
                               ImportTaskService taskService,
                               ImportContentExtractor importContentExtractor) {
        this.jobLauncher = jobLauncher;
        this.pdfImportJob = pdfImportJob;
        this.taskService = taskService;
        this.importContentExtractor = importContentExtractor;
    }

    public WordUploadStatusResponse submit(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is empty.");
        }
        String originalFileName = file.getOriginalFilename();
        if (!importContentExtractor.isSupported(originalFileName)) {
            throw new IllegalArgumentException("Unsupported file type. Supported types: pdf, txt, xlsx, csv, json, zip.");
        }
        ImportTask task = taskService.createTask(originalFileName, file.getContentType());
        String filePath = storeTempFile(task.getTaskId(), originalFileName, file);
        taskService.updateTotalUnits(task.getTaskId(), countUnits(filePath));
        runAsync(task.getTaskId(), filePath);
        return taskService.toWordUploadStatus(taskService.getTask(task.getTaskId()));
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

    private String storeTempFile(String taskId, String originalFileName, MultipartFile file) {
        try {
            String extension = resolveExtension(originalFileName);
            Path tempFile = Files.createTempFile("import-" + taskId + "-", extension.isBlank() ? ".bin" : "." + extension);
            file.transferTo(tempFile.toFile());
            return tempFile.toAbsolutePath().toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to store uploaded file.", ex);
        }
    }

    private String resolveExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1);
    }

    private int countUnits(String filePath) {
        List<ImportChunk> chunks = importContentExtractor.extractChunks(Path.of(filePath));
        return chunks.stream()
                .mapToInt(chunk -> Math.max(1, chunk.getUnitCount()))
                .sum();
    }
}
