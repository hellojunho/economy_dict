package com.economydict.service;

import com.economydict.batch.ImportChunk;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

@Service
public class ImportContentExtractor {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "txt", "xlsx", "csv", "json", "zip");
    private static final int MAX_CHARS_PER_CHUNK = 8_000;
    private static final int MAX_ZIP_DEPTH = 3;
    private static final int MAX_TERMS_PER_CHUNK = 200;

    private final ObjectMapper objectMapper;
    private final DataFormatter dataFormatter = new DataFormatter();

    public ImportContentExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isSupported(String fileName) {
        return SUPPORTED_EXTENSIONS.contains(extensionOf(fileName));
    }

    public String supportedExtensionsDescription() {
        return String.join(", ", SUPPORTED_EXTENSIONS);
    }

    public List<ImportChunk> extractChunks(Path filePath) {
        try {
            String fileName = filePath.getFileName() == null ? "upload" : filePath.getFileName().toString();
            byte[] bytes = Files.readAllBytes(filePath);
            List<ImportChunk> chunks = extractChunks(fileName, bytes, 0);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("No readable content found in the uploaded file.");
            }
            return chunks;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to parse uploaded file.", ex);
        }
    }

    private List<ImportChunk> extractChunks(String fileName, byte[] bytes, int zipDepth) throws IOException {
        String extension = extensionOf(fileName);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            return List.of();
        }

        return switch (extension) {
            case "pdf" -> extractPdfChunks(bytes, fileName);
            case "txt" -> chunkText(withSource(fileName, new String(bytes, StandardCharsets.UTF_8)));
            case "csv" -> chunkText(withSource(fileName, new String(bytes, StandardCharsets.UTF_8)));
            case "json" -> extractJsonChunks(bytes, fileName);
            case "xlsx" -> extractSpreadsheetChunks(bytes, fileName);
            case "zip" -> extractZipChunks(bytes, zipDepth + 1);
            default -> List.of();
        };
    }

    private List<ImportChunk> extractPdfChunks(byte[] bytes, String fileName) throws IOException {
        List<ImportChunk> chunks = new ArrayList<>();
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                chunks.addAll(chunkText(withSource(fileName + " page " + page, text)));
            }
        }
        return chunks;
    }

    private List<ImportChunk> extractJsonChunks(byte[] bytes, String fileName) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(bytes);
            List<OpenAiService.ExtractedTerm> directTerms = extractDirectJsonTerms(root);
            if (!directTerms.isEmpty()) {
                return chunkTerms(directTerms);
            }
            String text = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            return chunkText(withSource(fileName, text));
        } catch (Exception ex) {
            String text = new String(bytes, StandardCharsets.UTF_8);
            return chunkText(withSource(fileName, text));
        }
    }

    private List<ImportChunk> extractSpreadsheetChunks(byte[] bytes, String fileName) throws IOException {
        List<ImportChunk> chunks = new ArrayList<>();
        try (InputStream in = new ByteArrayInputStream(bytes); Workbook workbook = WorkbookFactory.create(in)) {
            for (Sheet sheet : workbook) {
                StringBuilder builder = new StringBuilder();
                builder.append("Source: ").append(fileName).append(" / Sheet: ").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    List<String> values = new ArrayList<>();
                    for (Cell cell : row) {
                        String value = dataFormatter.formatCellValue(cell);
                        if (!value.isBlank()) {
                            values.add(value.trim());
                        }
                    }
                    if (!values.isEmpty()) {
                        builder.append(String.join(" | ", values)).append('\n');
                    }
                }
                chunks.addAll(chunkText(builder.toString()));
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse spreadsheet file.", ex);
        }
        return chunks;
    }

    private List<ImportChunk> extractZipChunks(byte[] bytes, int zipDepth) throws IOException {
        if (zipDepth > MAX_ZIP_DEPTH) {
            return List.of();
        }

        List<ImportChunk> chunks = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                if (!isSupported(entryName)) {
                    continue;
                }
                byte[] entryBytes = zipInputStream.readAllBytes();
                chunks.addAll(extractChunks(entryName, entryBytes, zipDepth));
            }
        }
        return chunks;
    }

    private List<ImportChunk> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<ImportChunk> chunks = new ArrayList<>();
        String normalized = text.replace("\r\n", "\n").trim();
        StringBuilder current = new StringBuilder();
        for (String paragraph : normalized.split("\n\\s*\n")) {
            String candidate = paragraph.trim();
            if (candidate.isBlank()) {
                continue;
            }
            if (current.length() > 0 && current.length() + candidate.length() + 2 > MAX_CHARS_PER_CHUNK) {
                chunks.add(ImportChunk.text(current.toString().trim()));
                current.setLength(0);
            }
            if (candidate.length() > MAX_CHARS_PER_CHUNK) {
                for (int start = 0; start < candidate.length(); start += MAX_CHARS_PER_CHUNK) {
                    int end = Math.min(candidate.length(), start + MAX_CHARS_PER_CHUNK);
                    chunks.add(ImportChunk.text(candidate.substring(start, end).trim()));
                }
                continue;
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(candidate);
        }
        if (current.length() > 0) {
            chunks.add(ImportChunk.text(current.toString().trim()));
        }
        return chunks;
    }

    private List<OpenAiService.ExtractedTerm> extractDirectJsonTerms(JsonNode root) {
        if (root == null || !root.isObject() || root.isEmpty()) {
            return List.of();
        }

        List<OpenAiService.ExtractedTerm> terms = new ArrayList<>();
        var fields = root.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            String word = entry.getKey() == null ? "" : entry.getKey().trim();
            JsonNode value = entry.getValue();
            if (word.isBlank() || value == null || !value.isTextual()) {
                return List.of();
            }
            String meaning = value.asText("").trim();
            if (meaning.isBlank()) {
                continue;
            }
            OpenAiService.ExtractedTerm term = new OpenAiService.ExtractedTerm();
            term.setWord(word);
            term.setMeaning(meaning);
            term.setSource("JSON_IMPORT");
            terms.add(term);
        }
        return terms;
    }

    private List<ImportChunk> chunkTerms(List<OpenAiService.ExtractedTerm> terms) {
        if (terms == null || terms.isEmpty()) {
            return List.of();
        }

        List<ImportChunk> chunks = new ArrayList<>();
        for (int start = 0; start < terms.size(); start += MAX_TERMS_PER_CHUNK) {
            int end = Math.min(terms.size(), start + MAX_TERMS_PER_CHUNK);
            chunks.add(ImportChunk.extractedTerms(terms.subList(start, end)));
        }
        return chunks;
    }

    private String withSource(String sourceName, String text) {
        return "Source: " + sourceName + "\n" + (text == null ? "" : text);
    }

    private String extensionOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
