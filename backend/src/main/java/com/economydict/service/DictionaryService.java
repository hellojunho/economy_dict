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
    private final WordMetadataService wordMetadataService;
    private final DictionaryMeaningFormatService dictionaryMeaningFormatService;

    public DictionaryService(DictionaryEntryRepository repository,
                             WordMetadataService wordMetadataService,
                             DictionaryMeaningFormatService dictionaryMeaningFormatService) {
        this.repository = repository;
        this.wordMetadataService = wordMetadataService;
        this.dictionaryMeaningFormatService = dictionaryMeaningFormatService;
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
        entry.setMeaning(dictionaryMeaningFormatService.formatMeaning(dto.getWord(), dto.getMeaning()));
        entry.setEnglishWord(dto.getEnglishWord());
        entry.setEnglishMeaning(dto.getEnglishMeaning());
        entry.setFileType(wordMetadataService.resolveFileType(dto.getFileType()));
        entry.setSource(wordMetadataService.resolveSource(dto.getSourceId(), dto.getSourceName()));
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
        dto.setFileType(entry.getFileType() == null ? null : entry.getFileType().getCode());
        dto.setSourceId(entry.getSource() == null ? null : entry.getSource().getId());
        dto.setSourceName(entry.getSource() == null ? null : entry.getSource().getName());
        return dto;
    }
}
