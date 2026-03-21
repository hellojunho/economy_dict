package com.economydict.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "words", uniqueConstraints = {
        @UniqueConstraint(columnNames = "word")
})
public class DictionaryEntry extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(nullable = false, columnDefinition = "text")
    private String meaning;

    @Column(name = "english_word", length = 100)
    private String englishWord;

    @Column(name = "english_meaning", columnDefinition = "text")
    private String englishMeaning;

    @ManyToOne
    @JoinColumn(name = "file_type_code", referencedColumnName = "code")
    private FileType fileType;

    @ManyToOne
    @JoinColumn(name = "source_id")
    private WordSource source;

    public Long getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getEnglishWord() {
        return englishWord;
    }

    public void setEnglishWord(String englishWord) {
        this.englishWord = englishWord;
    }

    public String getEnglishMeaning() {
        return englishMeaning;
    }

    public void setEnglishMeaning(String englishMeaning) {
        this.englishMeaning = englishMeaning;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public WordSource getSource() {
        return source;
    }

    public void setSource(WordSource source) {
        this.source = source;
    }
}
