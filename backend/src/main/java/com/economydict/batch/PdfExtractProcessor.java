package com.economydict.batch;

import com.economydict.service.OpenAiService;
import java.util.Collections;
import java.util.List;
import org.springframework.batch.item.ItemProcessor;

public class PdfExtractProcessor implements ItemProcessor<ImportChunk, List<OpenAiService.ExtractedTerm>> {
    private final OpenAiService openAiService;
    private static final int MAX_CHARS = 12000;

    public PdfExtractProcessor(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @Override
    public List<OpenAiService.ExtractedTerm> process(ImportChunk item) {
        if (item == null) {
            return Collections.emptyList();
        }
        if (item.hasExtractedTerms()) {
            return item.getExtractedTerms();
        }
        String text = item.getText();
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        text = text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
        return openAiService.extractDictionaryTerms(text);
    }
}
