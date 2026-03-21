package com.economydict.service;

import com.economydict.dto.WordUploadStatusResponse;
import com.economydict.entity.DictionaryEntry;
import com.economydict.repository.DictionaryEntryRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EnglishTranslationJobService {
    private static final String TASK_NAME = "Words To English";
    private static final String TASK_TYPE = "ENGLISH_TRANSLATION";

    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final ImportTaskService importTaskService;
    private final OpenAiService openAiService;
    private final EnglishTranslationJobService asyncSelf;

    public EnglishTranslationJobService(DictionaryEntryRepository dictionaryEntryRepository,
                                        ImportTaskService importTaskService,
                                        OpenAiService openAiService,
                                        @Lazy EnglishTranslationJobService asyncSelf) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
        this.importTaskService = importTaskService;
        this.openAiService = openAiService;
        this.asyncSelf = asyncSelf;
    }

    public WordUploadStatusResponse submit() {
        String taskId = importTaskService.createTask(TASK_NAME, TASK_TYPE).getTaskId();
        int totalUnits = (int) Math.min(dictionaryEntryRepository.countPendingEnglishTranslations(), Integer.MAX_VALUE);
        importTaskService.updateTotalUnits(taskId, totalUnits);

        if (totalUnits == 0) {
            importTaskService.markFinished(taskId);
            return importTaskService.toWordUploadStatus(importTaskService.getTask(taskId));
        }

        asyncSelf.runAsync(taskId);
        return importTaskService.toWordUploadStatus(importTaskService.getTask(taskId));
    }

    @Async
    public void runAsync(String taskId) {
        try {
            importTaskService.markStarted(taskId);

            List<DictionaryEntry> entries = dictionaryEntryRepository.findPendingEnglishTranslations();
            int totalUnits = entries.size();
            importTaskService.updateTotalUnits(taskId, totalUnits);

            int processedUnits = 0;
            for (DictionaryEntry entry : entries) {
                try {
                    translate(entry);
                } catch (Exception ignored) {
                    // Continue with the remaining items and reflect progress on the task.
                } finally {
                    processedUnits += 1;
                    importTaskService.updateProgress(taskId, processedUnits, totalUnits);
                }
            }

            importTaskService.markFinished(taskId);
        } catch (Exception ex) {
            importTaskService.markFailed(taskId, importTaskService.stackTrace(ex));
        }
    }

    private void translate(DictionaryEntry entry) {
        OpenAiService.DefinitionResult result = openAiService.translateTermToEnglish(entry.getWord(), entry.getMeaning());
        boolean changed = false;
        String englishWord = normalize(result.getEnglishWord());
        String englishMeaning = normalize(result.getEnglishMeaning());

        if (!Objects.equals(normalize(entry.getEnglishWord()), englishWord) && englishWord != null) {
            entry.setEnglishWord(englishWord);
            changed = true;
        }
        if (!Objects.equals(normalize(entry.getEnglishMeaning()), englishMeaning) && englishMeaning != null) {
            entry.setEnglishMeaning(englishMeaning);
            changed = true;
        }

        if (changed) {
            dictionaryEntryRepository.save(entry);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
