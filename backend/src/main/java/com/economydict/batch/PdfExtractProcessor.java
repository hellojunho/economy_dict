package com.economydict.batch;

import com.economydict.service.OpenAiService;
import java.util.Collections;
import java.util.List;
import org.springframework.batch.item.ItemProcessor;

public class PdfExtractProcessor implements ItemProcessor<String, List<OpenAiService.ExtractedTerm>> {
    private final OpenAiService openAiService;
    private static final int MAX_CHARS = 12000;

    public PdfExtractProcessor(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @Override
    public List<OpenAiService.ExtractedTerm> process(String item) {
        if (item == null || item.isBlank()) {
            return Collections.emptyList();
        }
        String text = item.length() > MAX_CHARS ? item.substring(0, MAX_CHARS) : item;
        return openAiService.extractDictionaryTerms(text);
    }
}
