package com.economydict.batch;

import com.economydict.entity.DictionaryEntry;
import com.economydict.repository.DictionaryEntryRepository;
import com.economydict.service.OpenAiService;
import java.util.List;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class TermWriter implements ItemWriter<List<OpenAiService.ExtractedTerm>> {
    private final DictionaryEntryRepository dictionaryEntryRepository;

    public TermWriter(DictionaryEntryRepository dictionaryEntryRepository) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
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
                entry.setMeaning(term.getMeaning() == null ? "" : term.getMeaning());
                entry.setEnglishWord(term.getEnglishWord());
                entry.setEnglishMeaning(term.getEnglishMeaning());
                entry.setSource("AI_IMPORT");
                dictionaryEntryRepository.save(entry);
            }
        }
    }
}
