package com.economydict.service;

import com.economydict.dto.StockAdvisorMessageRequest;
import com.economydict.dto.StockAdvisorSourceResponse;
import com.economydict.dto.StockAdvisorThreadCreateRequest;
import com.economydict.dto.StockAdvisorThreadMessageDto;
import com.economydict.dto.StockAdvisorThreadResponse;
import com.economydict.dto.StockAdvisorThreadSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockAdvisorThreadService {
    private final UserService userService;
    private final StockAdvisorService stockAdvisorService;
    private final ObjectMapper objectMapper;
    private final Path storageRoot;

    public StockAdvisorThreadService(
            UserService userService,
            StockAdvisorService stockAdvisorService,
            ObjectMapper objectMapper,
            @Value("${app.stock-advisor.storage-path:backend/stock-advisor-chats}") String storagePath
    ) {
        this.userService = userService;
        this.stockAdvisorService = stockAdvisorService;
        this.objectMapper = objectMapper;
        this.storageRoot = Paths.get(storagePath);
    }

    public List<StockAdvisorThreadSummaryResponse> listThreads() {
        Path userDirectory = ensureUserDirectory();
        try (Stream<Path> paths = Files.list(userDirectory)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(this::readThread)
                    .sorted(Comparator.comparing(StockAdvisorThreadDocument::getUpdatedAt).reversed())
                    .map(this::toSummary)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read stock advisor thread list", ex);
        }
    }

    @Transactional
    public StockAdvisorThreadResponse createThread(StockAdvisorThreadCreateRequest request) {
        String symbol = stockAdvisorService.normalizeSymbol(request.getSymbol());
        String riskProfile = stockAdvisorService.normalizeLabel(request.getRiskProfile());
        String tradeStyle = stockAdvisorService.normalizeLabel(request.getTradeStyle());
        String notes = stockAdvisorService.normalizeNotes(request.getNotes());

        StockAdvisorThreadDocument document = new StockAdvisorThreadDocument();
        document.setThreadId(UUID.randomUUID().toString());
        document.setTitle(stockAdvisorService.buildThreadTitle(symbol, tradeStyle));
        document.setSymbol(symbol);
        document.setRiskProfile(riskProfile);
        document.setTradeStyle(tradeStyle);
        document.setNotes(notes);
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(document.getCreatedAt());
        document.setMessages(new ArrayList<>());

        OpenAiService.WebSearchResult result = stockAdvisorService.adviseConversation(
                symbol,
                riskProfile,
                tradeStyle,
                notes,
                List.of(new OpenAiService.ChatTurn(
                        "user",
                        stockAdvisorService.buildInitialAnalysisPrompt(symbol, riskProfile, tradeStyle, notes)
                ))
        );

        StockAdvisorThreadMessageDocument assistantMessage = new StockAdvisorThreadMessageDocument();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(result.getContent());
        assistantMessage.setCreatedAt(Instant.now());
        assistantMessage.setSources(stockAdvisorService.toSourceResponsesFromResult(result.getSources()));
        document.getMessages().add(assistantMessage);
        document.setUpdatedAt(assistantMessage.getCreatedAt());
        writeThread(document);
        return toResponse(document);
    }

    public StockAdvisorThreadResponse getThread(String threadId) {
        return toResponse(readThread(resolveThreadPath(threadId)));
    }

    @Transactional
    public StockAdvisorThreadResponse appendMessage(String threadId, StockAdvisorMessageRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Stock advisor message is required");
        }

        StockAdvisorThreadDocument document = readThread(resolveThreadPath(threadId));

        StockAdvisorThreadMessageDocument userMessage = new StockAdvisorThreadMessageDocument();
        userMessage.setRole("user");
        userMessage.setContent(request.getMessage().trim());
        userMessage.setCreatedAt(Instant.now());
        document.getMessages().add(userMessage);

        OpenAiService.WebSearchResult result = stockAdvisorService.adviseConversation(
                document.getSymbol(),
                document.getRiskProfile(),
                document.getTradeStyle(),
                document.getNotes(),
                document.getMessages().stream()
                        .map(message -> new OpenAiService.ChatTurn(message.getRole(), message.getContent()))
                        .collect(Collectors.toList())
        );

        StockAdvisorThreadMessageDocument assistantMessage = new StockAdvisorThreadMessageDocument();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(result.getContent());
        assistantMessage.setCreatedAt(Instant.now());
        assistantMessage.setSources(stockAdvisorService.toSourceResponsesFromResult(result.getSources()));
        document.getMessages().add(assistantMessage);
        document.setUpdatedAt(assistantMessage.getCreatedAt());
        writeThread(document);
        return toResponse(document);
    }

    public void deleteThread(String threadId) {
        try {
            Files.deleteIfExists(resolveThreadPath(threadId));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete stock advisor thread", ex);
        }
    }

    private Path ensureUserDirectory() {
        try {
            Files.createDirectories(storageRoot);
            Path userDirectory = storageRoot.resolve(safeUserId());
            Files.createDirectories(userDirectory);
            return userDirectory;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare stock advisor storage", ex);
        }
    }

    private Path resolveThreadPath(String threadId) {
        return ensureUserDirectory().resolve(threadId + ".json");
    }

    private StockAdvisorThreadDocument readThread(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), StockAdvisorThreadDocument.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Stock advisor thread not found");
        }
    }

    private void writeThread(StockAdvisorThreadDocument document) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resolveThreadPath(document.getThreadId()).toFile(), document);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist stock advisor thread", ex);
        }
    }

    private StockAdvisorThreadSummaryResponse toSummary(StockAdvisorThreadDocument document) {
        StockAdvisorThreadSummaryResponse response = new StockAdvisorThreadSummaryResponse();
        response.setThreadId(document.getThreadId());
        response.setTitle(document.getTitle());
        response.setSymbol(document.getSymbol());
        response.setRiskProfile(document.getRiskProfile());
        response.setTradeStyle(document.getTradeStyle());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setMessageCount(document.getMessages() == null ? 0 : document.getMessages().size());
        return response;
    }

    private StockAdvisorThreadResponse toResponse(StockAdvisorThreadDocument document) {
        StockAdvisorThreadResponse response = new StockAdvisorThreadResponse();
        response.setThreadId(document.getThreadId());
        response.setTitle(document.getTitle());
        response.setSymbol(document.getSymbol());
        response.setRiskProfile(document.getRiskProfile());
        response.setTradeStyle(document.getTradeStyle());
        response.setNotes(document.getNotes());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setMessages((document.getMessages() == null ? List.<StockAdvisorThreadMessageDocument>of() : document.getMessages()).stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList()));
        return response;
    }

    private StockAdvisorThreadMessageDto toMessageDto(StockAdvisorThreadMessageDocument document) {
        StockAdvisorThreadMessageDto dto = new StockAdvisorThreadMessageDto();
        dto.setRole(document.getRole());
        dto.setContent(document.getContent());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setSources(document.getSources());
        return dto;
    }

    private String safeUserId() {
        return userService.getCurrentUser().getUserId().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static class StockAdvisorThreadDocument {
        private String threadId;
        private String title;
        private String symbol;
        private String riskProfile;
        private String tradeStyle;
        private String notes;
        private Instant createdAt;
        private Instant updatedAt;
        private List<StockAdvisorThreadMessageDocument> messages;

        public String getThreadId() {
            return threadId;
        }

        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getRiskProfile() {
            return riskProfile;
        }

        public void setRiskProfile(String riskProfile) {
            this.riskProfile = riskProfile;
        }

        public String getTradeStyle() {
            return tradeStyle;
        }

        public void setTradeStyle(String tradeStyle) {
            this.tradeStyle = tradeStyle;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }

        public List<StockAdvisorThreadMessageDocument> getMessages() {
            return messages;
        }

        public void setMessages(List<StockAdvisorThreadMessageDocument> messages) {
            this.messages = messages;
        }
    }

    public static class StockAdvisorThreadMessageDocument {
        private String role;
        private String content;
        private Instant createdAt;
        private List<StockAdvisorSourceResponse> sources;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public List<StockAdvisorSourceResponse> getSources() {
            return sources;
        }

        public void setSources(List<StockAdvisorSourceResponse> sources) {
            this.sources = sources;
        }
    }
}
