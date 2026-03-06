package com.economydict.batch;

import com.economydict.service.ImportTaskService;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.item.ItemReadListener;

public class PdfProgressListener extends ItemListenerSupport<String, Object>
        implements StepExecutionListener, ItemReadListener<String> {

    private final ImportTaskService taskService;
    private StepExecution stepExecution;
    private String taskId;

    public PdfProgressListener(ImportTaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        this.taskId = stepExecution.getJobParameters().getString("taskId");
    }

    @Override
    public void afterRead(String item) {
        if (taskId == null || stepExecution == null) {
            return;
        }
        int totalPages = stepExecution.getExecutionContext().getInt("totalPages", 0);
        int processed = stepExecution.getReadCount();
        taskService.updateProgress(taskId, processed, totalPages);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }
}
