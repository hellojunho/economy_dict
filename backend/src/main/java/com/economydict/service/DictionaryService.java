package com.economydict.service;

import com.economydict.dto.DictionaryEntryDto;
import com.economydict.entity.DictionaryEntry;
import com.economydict.repository.DictionaryEntryRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DictionaryService {
    private final DictionaryEntryRepository repository;

    public DictionaryService(DictionaryEntryRepository repository) {
        this.repository = repository;
    }

    public List<DictionaryEntryDto> search(String keyword) {
        List<DictionaryEntry> entries = keyword == null || keyword.isBlank()
                ? repository.findAll()
                : repository.findByWordContainingIgnoreCase(keyword);
        return entries.stream().map(this::toDto).collect(Collectors.toList());
    }

    public DictionaryEntryDto create(DictionaryEntryDto dto) {
        DictionaryEntry entry = new DictionaryEntry();
        entry.setWord(dto.getWord());
        entry.setMeaning(dto.getMeaning());
        entry.setEnglishWord(dto.getEnglishWord());
        entry.setEnglishMeaning(dto.getEnglishMeaning());
        DictionaryEntry saved = repository.save(entry);
        return toDto(saved);
    }

    private DictionaryEntryDto toDto(DictionaryEntry entry) {
        DictionaryEntryDto dto = new DictionaryEntryDto();
        dto.setId(entry.getId());
        dto.setWord(entry.getWord());
        dto.setMeaning(entry.getMeaning());
        dto.setEnglishWord(entry.getEnglishWord());
        dto.setEnglishMeaning(entry.getEnglishMeaning());
        return dto;
    }
}
