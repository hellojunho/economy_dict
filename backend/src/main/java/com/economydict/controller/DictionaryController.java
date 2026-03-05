package com.economydict.controller;

import com.economydict.dto.DictionaryEntryDto;
import com.economydict.service.DictionaryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {
    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public ResponseEntity<List<DictionaryEntryDto>> search(@RequestParam(value = "q", required = false) String query) {
        return ResponseEntity.ok(dictionaryService.search(query));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DictionaryEntryDto> create(@Valid @RequestBody DictionaryEntryDto dto) {
        return ResponseEntity.ok(dictionaryService.create(dto));
    }
}
