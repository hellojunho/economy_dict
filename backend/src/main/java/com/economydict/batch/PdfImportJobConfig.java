package com.economydict.batch;

import com.economydict.service.ImportContentExtractor;
import com.economydict.service.ImportTaskService;
import com.economydict.service.DictionaryMeaningFormatService;
import com.economydict.service.OpenAiService;
import com.economydict.repository.DictionaryEntryRepository;
import com.economydict.repository.FileTypeRepository;
import com.economydict.repository.WordSourceRepository;
import java.io.File;
import java.util.stream.Collectors;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.transaction.PlatformTransactionManager;
import com.economydict.batch.PdfProgressListener;

@Configuration
@EnableBatchProcessing
public class PdfImportJobConfig {

    @Bean
    public Job pdfImportJob(JobRepository jobRepository, Step pdfImportStep, JobExecutionListener jobListener) {
        return new JobBuilder("pdfImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobListener)
                .start(pdfImportStep)
                .build();
    }

    @Bean
    public Step pdfImportStep(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              ItemReader<ImportChunk> pdfChunkReader,
                              ItemProcessor<ImportChunk, java.util.List<OpenAiService.ExtractedTerm>> pdfExtractProcessor,
                              ItemWriter<java.util.List<OpenAiService.ExtractedTerm>> termWriter,
                              StepExecutionListener stepListener,
                              PdfProgressListener pdfProgressListener) {
        return new StepBuilder("pdfImportStep", jobRepository)
                .<ImportChunk, java.util.List<OpenAiService.ExtractedTerm>>chunk(1, transactionManager)
                .reader(pdfChunkReader)
                .processor(pdfExtractProcessor)
                .writer(termWriter)
                .listener(stepListener)
                .listener((org.springframework.batch.core.ItemReadListener<ImportChunk>) pdfProgressListener)
                .build();
    }

    @Bean
    @StepScope
    public PdfChunkReader pdfChunkReader(@Value("#{jobParameters['filePath']}") String filePath,
                                         ImportContentExtractor importContentExtractor) {
        return new PdfChunkReader(filePath, importContentExtractor);
    }

    @Bean
    @StepScope
    public ItemProcessor<ImportChunk, java.util.List<OpenAiService.ExtractedTerm>> pdfExtractProcessor(
            OpenAiService openAiService,
            @Value("#{jobParameters['uploadModel']}") String uploadModel) {
        return new PdfExtractProcessor(openAiService, uploadModel);
    }

    @Bean
    @StepScope
    public ItemWriter<java.util.List<OpenAiService.ExtractedTerm>> termWriter(
            DictionaryEntryRepository dictionaryEntryRepository,
            FileTypeRepository fileTypeRepository,
            WordSourceRepository wordSourceRepository,
            DictionaryMeaningFormatService dictionaryMeaningFormatService,
            @Value("#{jobParameters['sourceId']}") String sourceIdValue) {
        Long sourceId = sourceIdValue == null || sourceIdValue.isBlank() ? null : Long.valueOf(sourceIdValue);
        return new TermWriter(dictionaryEntryRepository, fileTypeRepository, wordSourceRepository, sourceId, dictionaryMeaningFormatService);
    }

    @Bean
    public JobExecutionListener jobListener(ImportTaskService taskService) {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                String taskId = jobExecution.getJobParameters().getString("taskId");
                taskService.markStarted(taskId);
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                String taskId = jobExecution.getJobParameters().getString("taskId");
                String filePath = jobExecution.getJobParameters().getString("filePath");
                if (jobExecution.getAllFailureExceptions().isEmpty()) {
                    taskService.markFinished(taskId);
                } else {
                    String trace = jobExecution.getAllFailureExceptions().stream()
                            .map(taskService::stackTrace)
                            .collect(Collectors.joining("\n"));
                    taskService.markFailed(taskId, trace);
                }
                if (filePath != null) {
                    new File(filePath).delete();
                }
            }
        };
    }

    @Bean
    public StepExecutionListener stepListener(ImportTaskService taskService) {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                String taskId = stepExecution.getJobParameters().getString("taskId");
                taskService.markPending(taskId);
            }

            @Override
            public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {
                return stepExecution.getExitStatus();
            }
        };
    }

    @Bean
    @StepScope
    public PdfProgressListener pdfProgressListener(ImportTaskService taskService) {
        return new PdfProgressListener(taskService);
    }
}
