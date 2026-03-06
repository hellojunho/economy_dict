package com.economydict.dto;

public class PdfImportResponse {
    private int extractedCount;
    private int createdCount;
    private int skippedCount;

    public PdfImportResponse(int extractedCount, int createdCount, int skippedCount) {
        this.extractedCount = extractedCount;
        this.createdCount = createdCount;
        this.skippedCount = skippedCount;
    }

    public int getExtractedCount() {
        return extractedCount;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }
}
