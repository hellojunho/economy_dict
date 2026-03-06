package com.economydict.batch;

import com.economydict.service.ImportTaskService;
import com.economydict.service.OpenAiService;
import com.economydict.repository.DictionaryEntryRepository;
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
                              ItemReader<String> pdfChunkReader,
                              ItemProcessor<String, java.util.List<OpenAiService.ExtractedTerm>> pdfExtractProcessor,
                              ItemWriter<java.util.List<OpenAiService.ExtractedTerm>> termWriter,
                              StepExecutionListener stepListener) {
        return new StepBuilder("pdfImportStep", jobRepository)
                .<String, java.util.List<OpenAiService.ExtractedTerm>>chunk(1, transactionManager)
                .reader(pdfChunkReader)
                .processor(pdfExtractProcessor)
                .writer(termWriter)
                .listener(stepListener)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<String> pdfChunkReader(@Value("#{jobParameters['filePath']}") String filePath) {
        return new PdfChunkReader(filePath);
    }

    @Bean
    public ItemProcessor<String, java.util.List<OpenAiService.ExtractedTerm>> pdfExtractProcessor(OpenAiService openAiService) {
        return new PdfExtractProcessor(openAiService);
    }

    @Bean
    public ItemWriter<java.util.List<OpenAiService.ExtractedTerm>> termWriter(DictionaryEntryRepository dictionaryEntryRepository) {
        return new TermWriter(dictionaryEntryRepository);
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
}
