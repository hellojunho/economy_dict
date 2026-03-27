package com.economydict.batch;

import com.economydict.service.OpenAiService;
import java.util.Collections;
import java.util.List;
import org.springframework.batch.item.ItemProcessor;

public class PdfExtractProcessor implements ItemProcessor<ImportChunk, List<OpenAiService.ExtractedTerm>> {
    private final OpenAiService openAiService;
    private final String uploadModel;
    private static final int MAX_CHARS = 12000;

    public PdfExtractProcessor(OpenAiService openAiService, String uploadModel) {
        this.openAiService = openAiService;
        this.uploadModel = uploadModel;
    }

    @Override
    public List<OpenAiService.ExtractedTerm> process(ImportChunk item) {
        if (item == null) {
            return Collections.emptyList();
        }
        if (item.hasExtractedTerms()) {
            return item.getExtractedTerms().stream()
                    .map(this::formatImportedTerm)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
        String text = item.getText();
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        text = text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
        return openAiService.extractDictionaryTerms(text, uploadModel);
    }

    private OpenAiService.ExtractedTerm formatImportedTerm(OpenAiService.ExtractedTerm term) {
        if (term == null) {
            return null;
        }
        OpenAiService.ExtractedTerm formatted = new OpenAiService.ExtractedTerm();
        formatted.setWord(term.getWord());
        formatted.setSource(term.getSource());

        if ("JSON_IMPORT".equals(term.getSource())) {
            OpenAiService.DefinitionResult summary = openAiService.summarizeUploadedTerm(term.getWord(), term.getMeaning(), uploadModel);
            formatted.setMeaning(summary.getMeaning());
            formatted.setEnglishWord(selectValue(summary.getEnglishWord(), term.getEnglishWord()));
            formatted.setEnglishMeaning(selectValue(summary.getEnglishMeaning(), term.getEnglishMeaning()));
            return formatted;
        }

        formatted.setMeaning(term.getMeaning());
        if (isBlank(term.getEnglishWord()) || isBlank(term.getEnglishMeaning())) {
            OpenAiService.DefinitionResult enriched = openAiService.enrichImportedTerm(term.getWord(), term.getMeaning(), uploadModel);
            formatted.setEnglishWord(selectValue(term.getEnglishWord(), enriched.getEnglishWord()));
            formatted.setEnglishMeaning(selectValue(term.getEnglishMeaning(), enriched.getEnglishMeaning()));
            return formatted;
        }

        formatted.setEnglishWord(term.getEnglishWord());
        formatted.setEnglishMeaning(term.getEnglishMeaning());
        return formatted;
    }

    private String selectValue(String primary, String secondary) {
        return !isBlank(primary) ? primary : secondary;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
