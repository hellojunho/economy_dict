package com.economydict.service;

import com.economydict.dto.SourceOptionDto;
import com.economydict.dto.FileTypeOptionDto;
import com.economydict.entity.FileType;
import com.economydict.entity.WordSource;
import com.economydict.repository.FileTypeRepository;
import com.economydict.repository.WordSourceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WordMetadataService {
    private final FileTypeRepository fileTypeRepository;
    private final WordSourceRepository wordSourceRepository;

    public WordMetadataService(FileTypeRepository fileTypeRepository, WordSourceRepository wordSourceRepository) {
        this.fileTypeRepository = fileTypeRepository;
        this.wordSourceRepository = wordSourceRepository;
    }

    public FileType resolveFileType(String code) {
        String normalized = normalizeCode(code);
        return fileTypeRepository.findById(normalized)
                .orElseGet(() -> {
                    FileType fileType = new FileType();
                    fileType.setCode(normalized);
                    fileType.setDisplayName(normalized);
                    return fileTypeRepository.save(fileType);
                });
    }

    public WordSource resolveSource(Long sourceId, String sourceName) {
        if (sourceId != null) {
            return wordSourceRepository.findById(sourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Source not found"));
        }
        if (sourceName == null || sourceName.isBlank()) {
            return null;
        }
        String normalized = sourceName.trim();
        return wordSourceRepository.findByNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    WordSource source = new WordSource();
                    source.setName(normalized);
                    return wordSourceRepository.save(source);
                });
    }

    public List<SourceOptionDto> listSources() {
        return wordSourceRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(WordSource::getName, String.CASE_INSENSITIVE_ORDER))
                .map(source -> new SourceOptionDto(source.getId(), source.getName()))
                .toList();
    }

    public List<FileTypeOptionDto> listFileTypes() {
        return fileTypeRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(FileType::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(fileType -> new FileTypeOptionDto(fileType.getCode(), fileType.getDisplayName()))
                .toList();
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return "MANUAL";
        }
        return code.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    }
}
