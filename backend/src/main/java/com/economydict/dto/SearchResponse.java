package com.economydict.dto;

public class SearchResponse {
    private String word;
    private String meaning;
    private String englishWord;

    public SearchResponse(String word, String meaning, String englishWord) {
        this.word = word;
        this.meaning = meaning;
        this.englishWord = englishWord;
    }

    public String getWord() {
        return word;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getEnglishWord() {
        return englishWord;
    }
}
