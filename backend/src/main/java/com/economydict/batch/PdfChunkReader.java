package com.economydict.batch;

import com.economydict.service.ImportContentExtractor;
import java.nio.file.Path;
import java.util.List;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemStreamSupport;

public class PdfChunkReader extends ItemStreamSupport implements ItemStreamReader<ImportChunk> {
    private final String filePath;
    private final ImportContentExtractor importContentExtractor;
    private List<ImportChunk> chunks;
    private int currentIndex;

    public PdfChunkReader(String filePath, ImportContentExtractor importContentExtractor) {
        this.filePath = filePath;
        this.importContentExtractor = importContentExtractor;
    }

    @Override
    public ImportChunk read() {
        if (chunks == null || currentIndex >= chunks.size()) {
            return null;
        }
        return chunks.get(currentIndex++);
    }

    @Override
    public void open(ExecutionContext executionContext) {
        chunks = importContentExtractor.extractChunks(Path.of(filePath));
        currentIndex = 0;
        int totalUnits = chunks.stream()
                .mapToInt(chunk -> Math.max(1, chunk.getUnitCount()))
                .sum();
        executionContext.putInt("totalUnits", totalUnits);
    }

    @Override
    public void close() {
        chunks = null;
        currentIndex = 0;
    }
}
