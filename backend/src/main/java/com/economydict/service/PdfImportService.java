package com.economydict.service;

import com.economydict.dto.PdfImportResponse;
import com.economydict.entity.DictionaryEntry;
import com.economydict.repository.DictionaryEntryRepository;
import java.io.InputStream;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfImportService {
    private static final int MAX_CHARS = 12000;

    private final OpenAiService openAiService;
    private final DictionaryEntryRepository dictionaryEntryRepository;

    public PdfImportService(OpenAiService openAiService, DictionaryEntryRepository dictionaryEntryRepository) {
        this.openAiService = openAiService;
        this.dictionaryEntryRepository = dictionaryEntryRepository;
    }

    public PdfImportResponse importPdf(MultipartFile file) {
        String text = extractText(file);
        List<OpenAiService.ExtractedTerm> terms = openAiService.extractDictionaryTerms(text);
        int created = 0;
        int skipped = 0;
        for (OpenAiService.ExtractedTerm term : terms) {
            if (term.getWord() == null || term.getWord().isBlank()) {
                continue;
            }
            String word = term.getWord().trim();
            if (dictionaryEntryRepository.existsByWordIgnoreCase(word)) {
                skipped++;
                continue;
            }
            DictionaryEntry entry = new DictionaryEntry();
            entry.setWord(word);
            entry.setMeaning(term.getMeaning() == null ? "" : term.getMeaning());
            entry.setEnglishWord(term.getEnglishWord());
            entry.setEnglishMeaning(term.getEnglishMeaning());
            dictionaryEntryRepository.save(entry);
            created++;
        }
        return new PdfImportResponse(terms.size(), created, skipped);
    }

    private String extractText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream(); PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            if (text.length() > MAX_CHARS) {
                return text.substring(0, MAX_CHARS);
            }
            return text;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read PDF", ex);
        }
    }
}
