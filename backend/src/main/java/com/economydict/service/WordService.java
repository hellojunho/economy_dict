package com.economydict.service;

import com.economydict.dto.PagedResponse;
import com.economydict.dto.WordResponse;
import com.economydict.entity.DictionaryEntry;
import com.economydict.repository.DictionaryEntryRepository;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class WordService {
    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final OpenAiService openAiService;
    private final WordMetadataService wordMetadataService;

    public WordService(DictionaryEntryRepository dictionaryEntryRepository,
                       OpenAiService openAiService,
                       WordMetadataService wordMetadataService) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
        this.openAiService = openAiService;
        this.wordMetadataService = wordMetadataService;
    }

    public PagedResponse<WordResponse> getWords(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DictionaryEntry> result = (query == null || query.isBlank())
                ? dictionaryEntryRepository.findAll(pageable)
                : dictionaryEntryRepository.findByWordContainingIgnoreCaseOrMeaningContainingIgnoreCase(query, query, pageable);
        PagedResponse<WordResponse> response = new PagedResponse<>();
        response.setContent(result.getContent().stream().map(this::toResponse).collect(Collectors.toList()));
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        return response;
    }

    public WordResponse getWord(Long id) {
        DictionaryEntry entry = dictionaryEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Word not found"));
        return toResponse(entry);
    }

    public WordResponse lookupWord(String term) {
        if (term == null || term.isBlank()) {
            throw new IllegalArgumentException("Word query is required");
        }
        String normalized = term.trim();
        DictionaryEntry existing = dictionaryEntryRepository.findByWordIgnoreCase(normalized).orElse(null);
        if (existing != null && existing.getMeaning() != null && !existing.getMeaning().isBlank()) {
            return toResponse(existing);
        }

        OpenAiService.DefinitionResult result = openAiService.getDefinition(normalized);
        if (result == null || result.getMeaning() == null || result.getMeaning().isBlank()) {
            throw new IllegalStateException("Failed to generate word definition");
        }

        DictionaryEntry entry = existing != null ? existing : new DictionaryEntry();
        entry.setWord(normalized);
        entry.setMeaning(result.getMeaning());
        entry.setEnglishWord(result.getEnglishWord());
        entry.setEnglishMeaning(result.getEnglishMeaning());
        entry.setFileType(wordMetadataService.resolveFileType("AI_LOOKUP"));
        return toResponse(dictionaryEntryRepository.save(entry));
    }

    public WordResponse toResponse(DictionaryEntry entry) {
        WordResponse response = new WordResponse();
        response.setId(entry.getId());
        response.setWord(entry.getWord());
        response.setMeaning(entry.getMeaning());
        response.setEnglishWord(entry.getEnglishWord());
        return response;
    }
}
