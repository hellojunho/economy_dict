package com.economydict.service;

import com.economydict.dto.SearchResponse;
import com.economydict.dto.WordResponse;
import org.springframework.stereotype.Service;

@Service
public class SearchService {
    private final WordService wordService;

    public SearchService(WordService wordService) {
        this.wordService = wordService;
    }

    public SearchResponse search(String term) {
        WordResponse response = wordService.lookupWord(term);
        return new SearchResponse(response.getWord(), response.getMeaning(), response.getEnglishWord());
    }
}
