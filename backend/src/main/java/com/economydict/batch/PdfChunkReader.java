package com.economydict.batch;

import java.io.File;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemStreamSupport;

public class PdfChunkReader extends ItemStreamSupport implements ItemStreamReader<String> {
    private final String filePath;
    private PDDocument document;
    private int currentPage;
    private int totalPages;

    public PdfChunkReader(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String read() throws Exception {
        if (document == null || currentPage > totalPages) {
            return null;
        }
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setStartPage(currentPage);
        stripper.setEndPage(currentPage);
        String text = stripper.getText(document);
        currentPage++;
        return text;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        try {
            document = PDDocument.load(new File(filePath));
            totalPages = document.getNumberOfPages();
            currentPage = 1;
            executionContext.putInt("totalPages", totalPages);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to open PDF", e);
        }
    }

    @Override
    public void close() {
        if (document != null) {
            try {
                document.close();
            } catch (Exception ignored) {
            }
        }
    }
}
