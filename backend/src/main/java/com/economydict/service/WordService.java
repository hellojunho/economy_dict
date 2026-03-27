package com.economydict.service;

import com.economydict.dto.PagedResponse;
import com.economydict.dto.WordResponse;
import com.economydict.entity.DictionaryEntry;
import com.economydict.repository.DictionaryEntryRepository;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class WordService {
    private final DictionaryEntryRepository dictionaryEntryRepository;
    private final OpenAiService openAiService;
    private final WordMetadataService wordMetadataService;
    private final DictionaryMeaningFormatService dictionaryMeaningFormatService;

    public WordService(DictionaryEntryRepository dictionaryEntryRepository,
                       OpenAiService openAiService,
                       WordMetadataService wordMetadataService,
                       DictionaryMeaningFormatService dictionaryMeaningFormatService) {
        this.dictionaryEntryRepository = dictionaryEntryRepository;
        this.openAiService = openAiService;
        this.wordMetadataService = wordMetadataService;
        this.dictionaryMeaningFormatService = dictionaryMeaningFormatService;
    }

    private static final String[] CHOSUNG_STARTS = {
        "가","나","다","라","마","바","사","아","자","차","카","타","파","하"
    };
    private static final String[] CHOSUNG_KEYS = {
        "ㄱ","ㄴ","ㄷ","ㄹ","ㅁ","ㅂ","ㅅ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"
    };

    public PagedResponse<WordResponse> getWords(String query, String initial, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "word"));
        Page<DictionaryEntry> result;
        if (query != null && !query.isBlank()) {
            result = dictionaryEntryRepository.findByWordContainingIgnoreCaseOrMeaningContainingIgnoreCase(query, query, pageable);
        } else if (initial != null && !initial.isBlank()) {
            int idx = -1;
            for (int i = 0; i < CHOSUNG_KEYS.length; i++) {
                if (CHOSUNG_KEYS[i].equals(initial.trim())) { idx = i; break; }
            }
            if (idx >= 0) {
                String start = CHOSUNG_STARTS[idx];
                String end = idx + 1 < CHOSUNG_STARTS.length ? CHOSUNG_STARTS[idx + 1] : "힣\uFFFF";
                result = dictionaryEntryRepository.findByWordGreaterThanEqualAndWordLessThan(start, end, pageable);
            } else {
                result = dictionaryEntryRepository.findAll(pageable);
            }
        } else {
            result = dictionaryEntryRepository.findAll(pageable);
        }
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
        entry.setMeaning(dictionaryMeaningFormatService.formatMeaning(normalized, result.getMeaning()));
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
