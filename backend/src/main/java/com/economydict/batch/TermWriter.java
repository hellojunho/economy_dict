package com.economydict.batch;

import com.economydict.entity.DictionaryEntry;
import com.economydict.entity.FileType;
import com.economydict.entity.WordSource;
import com.economydict.repository.FileTypeRepository;
import com.economydict.repository.DictionaryEntryRepository;
import com.economydict.repository.WordSourceRepository;
import com.economydict.service.DictionaryMeaningFormatService;
import com.economydict.service.OpenAiService;
import java.util.List;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class TermWriter implements ItemWriter<List<OpenAiService.ExtractedTerm>> {
    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final FileTypeRepository fileTypeRepository;
    private final WordSourceRepository wordSourceRepository;
    private final Long sourceId;
    private final DictionaryMeaningFormatService dictionaryMeaningFormatService;

    public TermWriter(DictionaryEntryRepository dictionaryEntryRepository,
                      FileTypeRepository fileTypeRepository,
                      WordSourceRepository wordSourceRepository,
                      Long sourceId,
                      DictionaryMeaningFormatService dictionaryMeaningFormatService) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
        this.fileTypeRepository = fileTypeRepository;
        this.wordSourceRepository = wordSourceRepository;
        this.sourceId = sourceId;
        this.dictionaryMeaningFormatService = dictionaryMeaningFormatService;
    }

    @Override
    public void write(Chunk<? extends List<OpenAiService.ExtractedTerm>> items) {
        for (List<OpenAiService.ExtractedTerm> terms : items) {
            for (OpenAiService.ExtractedTerm term : terms) {
                if (term.getWord() == null || term.getWord().isBlank()) {
                    continue;
                }
                String word = term.getWord().trim();
                if (dictionaryEntryRepository.existsByWordIgnoreCase(word)) {
                    continue;
                }
                DictionaryEntry entry = new DictionaryEntry();
                entry.setWord(word);
                entry.setMeaning(dictionaryMeaningFormatService.formatMeaning(word, term.getMeaning() == null ? "" : term.getMeaning()));
                entry.setEnglishWord(term.getEnglishWord());
                entry.setEnglishMeaning(term.getEnglishMeaning());
                String fileTypeCode = term.getSource() == null || term.getSource().isBlank() ? "AI_IMPORT" : term.getSource();
                entry.setFileType(resolveFileType(fileTypeCode));
                entry.setSource(resolveWordSource());
                dictionaryEntryRepository.save(entry);
            }
        }
    }

    private FileType resolveFileType(String code) {
        return fileTypeRepository.findById(code)
                .orElseGet(() -> {
                    FileType fileType = new FileType();
                    fileType.setCode(code);
                    fileType.setDisplayName(code);
                    return fileTypeRepository.save(fileType);
                });
    }

    private WordSource resolveWordSource() {
        if (sourceId == null) {
            return null;
        }
        return wordSourceRepository.findById(sourceId).orElse(null);
    }
}
