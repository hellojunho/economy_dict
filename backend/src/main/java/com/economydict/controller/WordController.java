package com.economydict.controller;

import com.economydict.dto.PagedResponse;
import com.economydict.dto.WordResponse;
import com.economydict.service.WordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/words")
public class WordController {
    private final WordService wordService;

    public WordController(WordService wordService) {
        this.wordService = wordService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<WordResponse>> list(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "initial", required = false) String initial,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(wordService.getWords(query, initial, page, size));
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<WordResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(wordService.getWord(id));
    }

    @GetMapping("/lookup")
    public ResponseEntity<WordResponse> lookup(@RequestParam("q") String query) {
        return ResponseEntity.ok(wordService.lookupWord(query));
    }
}
