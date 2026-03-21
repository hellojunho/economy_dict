package com.economydict.batch;

import com.economydict.service.OpenAiService;
import java.util.List;

public class ImportChunk {
    private final String text;
    private final List<OpenAiService.ExtractedTerm> extractedTerms;
    private final int unitCount;

    private ImportChunk(String text, List<OpenAiService.ExtractedTerm> extractedTerms, int unitCount) {
        this.text = text;
        this.extractedTerms = extractedTerms;
        this.unitCount = unitCount;
    }

    public static ImportChunk text(String text) {
        return new ImportChunk(text, List.of(), 1);
    }

    public static ImportChunk extractedTerms(List<OpenAiService.ExtractedTerm> extractedTerms) {
        List<OpenAiService.ExtractedTerm> safeTerms = extractedTerms == null ? List.of() : List.copyOf(extractedTerms);
        return new ImportChunk(null, safeTerms, safeTerms.size());
    }

    public String getText() {
        return text;
    }

    public List<OpenAiService.ExtractedTerm> getExtractedTerms() {
        return extractedTerms;
    }

    public boolean hasExtractedTerms() {
        return extractedTerms != null && !extractedTerms.isEmpty();
    }

    public int getUnitCount() {
        return unitCount;
    }
}
