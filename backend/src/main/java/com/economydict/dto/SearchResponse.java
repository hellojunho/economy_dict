package com.economydict.dto;

public class SearchResponse {
    private String word;
    private String meaning;
    private String englishWord;
    private String englishMeaning;
    private String source;

    public SearchResponse(String word, String meaning, String englishWord, String englishMeaning, String source) {
        this.word = word;
        this.meaning = meaning;
        this.englishWord = englishWord;
        this.englishMeaning = englishMeaning;
        this.source = source;
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

    public String getEnglishMeaning() {
        return englishMeaning;
    }

    public String getSource() {
        return source;
    }
}
