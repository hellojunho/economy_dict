package com.economydict.batch;

import com.economydict.service.ImportTaskService;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.listener.ItemListenerSupport;

public class PdfProgressListener extends ItemListenerSupport<ImportChunk, Object>
        implements StepExecutionListener, ItemReadListener<ImportChunk> {

    private final ImportTaskService taskService;
    private StepExecution stepExecution;
    private String taskId;
    private int processedUnits;

    public PdfProgressListener(ImportTaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        this.taskId = stepExecution.getJobParameters().getString("taskId");
        this.processedUnits = 0;
    }

    @Override
    public void afterRead(ImportChunk item) {
        if (taskId == null || stepExecution == null) {
            return;
        }
        int totalUnits = stepExecution.getExecutionContext().getInt("totalUnits", 0);
        processedUnits += item == null ? 0 : Math.max(1, item.getUnitCount());
        taskService.updateProgress(taskId, processedUnits, totalUnits);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }
}
