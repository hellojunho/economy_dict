package com.economydict.service;

import com.economydict.dto.AiInvestMessageRequest;
import com.economydict.dto.AiInvestThreadCreateRequest;
import com.economydict.dto.AiInvestThreadMessageDto;
import com.economydict.dto.AiInvestThreadResponse;
import com.economydict.dto.AiInvestThreadSummaryResponse;
import com.economydict.dto.StockAdvisorSourceResponse;
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
public class AiInvestThreadService {

    private final UserService userService;
    private final AiInvestService aiInvestService;
    private final ObjectMapper objectMapper;
    private final Path storageRoot;

    public AiInvestThreadService(
            UserService userService,
            AiInvestService aiInvestService,
            ObjectMapper objectMapper,
            @Value("${app.ai-invest.storage-path:backend/ai-invest-chats}") String storagePath
    ) {
        this.userService = userService;
        this.aiInvestService = aiInvestService;
        this.objectMapper = objectMapper;
        this.storageRoot = Paths.get(storagePath);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public List<AiInvestThreadSummaryResponse> listThreads() {
        Path userDirectory = ensureUserDirectory();
        try (Stream<Path> paths = Files.list(userDirectory)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(this::readThread)
                    .sorted(Comparator.comparing(AiInvestThreadDocument::getUpdatedAt).reversed())
                    .map(this::toSummary)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read AI invest thread list", ex);
        }
    }

    @Transactional
    public AiInvestThreadResponse createThread(AiInvestThreadCreateRequest request) {
        String stockName = aiInvestService.normalizeStockName(request.getStockName());
        String market = aiInvestService.normalizeMarket(request.getMarket());
        String riskProfile = aiInvestService.normalizeLabel(request.getRiskProfile());
        String tradeStyle = aiInvestService.normalizeLabel(request.getTradeStyle());
        String notes = aiInvestService.normalizeNotes(request.getNotes());

        AiInvestThreadDocument document = new AiInvestThreadDocument();
        document.setThreadId(UUID.randomUUID().toString());
        document.setTitle(aiInvestService.buildThreadTitle(stockName, market, tradeStyle));
        document.setStockName(stockName);
        document.setMarket(market);
        document.setRiskProfile(riskProfile);
        document.setTradeStyle(tradeStyle);
        document.setNotes(notes);
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(document.getCreatedAt());
        document.setMessages(new ArrayList<>());

        OpenAiService.WebSearchResult result = aiInvestService.adviseConversation(
                stockName,
                market,
                riskProfile,
                tradeStyle,
                notes,
                List.of(new OpenAiService.ChatTurn(
                        "user",
                        aiInvestService.buildInitialPrompt(stockName, market, riskProfile, tradeStyle, notes)
                ))
        );

        AiInvestThreadMessageDocument assistantMessage = new AiInvestThreadMessageDocument();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(result.getContent());
        assistantMessage.setCreatedAt(Instant.now());
        assistantMessage.setSources(aiInvestService.toSourceResponsesFromResult(result.getSources()));
        document.getMessages().add(assistantMessage);
        document.setUpdatedAt(assistantMessage.getCreatedAt());
        writeThread(document);
        return toResponse(document);
    }

    public AiInvestThreadResponse getThread(String threadId) {
        return toResponse(readThread(resolveThreadPath(threadId)));
    }

    @Transactional
    public AiInvestThreadResponse appendMessage(String threadId, AiInvestMessageRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("AI invest message is required");
        }

        AiInvestThreadDocument document = readThread(resolveThreadPath(threadId));

        AiInvestThreadMessageDocument userMessage = new AiInvestThreadMessageDocument();
        userMessage.setRole("user");
        userMessage.setContent(request.getMessage().trim());
        userMessage.setCreatedAt(Instant.now());
        document.getMessages().add(userMessage);

        OpenAiService.WebSearchResult result = aiInvestService.adviseConversation(
                document.getStockName(),
                document.getMarket(),
                document.getRiskProfile(),
                document.getTradeStyle(),
                document.getNotes(),
                document.getMessages().stream()
                        .map(message -> new OpenAiService.ChatTurn(message.getRole(), message.getContent()))
                        .collect(Collectors.toList())
        );

        AiInvestThreadMessageDocument assistantMessage = new AiInvestThreadMessageDocument();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(result.getContent());
        assistantMessage.setCreatedAt(Instant.now());
        assistantMessage.setSources(aiInvestService.toSourceResponsesFromResult(result.getSources()));
        document.getMessages().add(assistantMessage);
        document.setUpdatedAt(assistantMessage.getCreatedAt());
        writeThread(document);
        return toResponse(document);
    }

    public void deleteThread(String threadId) {
        try {
            Files.deleteIfExists(resolveThreadPath(threadId));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete AI invest thread", ex);
        }
    }

    // -------------------------------------------------------------------------
    // Storage helpers
    // -------------------------------------------------------------------------

    private Path ensureUserDirectory() {
        try {
            Files.createDirectories(storageRoot);
            Path userDirectory = storageRoot.resolve(safeUserId());
            Files.createDirectories(userDirectory);
            return userDirectory;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare AI invest storage", ex);
        }
    }

    private Path resolveThreadPath(String threadId) {
        return ensureUserDirectory().resolve(threadId + ".json");
    }

    private AiInvestThreadDocument readThread(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), AiInvestThreadDocument.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("AI invest thread not found");
        }
    }

    private void writeThread(AiInvestThreadDocument document) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(resolveThreadPath(document.getThreadId()).toFile(), document);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist AI invest thread", ex);
        }
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private AiInvestThreadSummaryResponse toSummary(AiInvestThreadDocument document) {
        AiInvestThreadSummaryResponse response = new AiInvestThreadSummaryResponse();
        response.setThreadId(document.getThreadId());
        response.setTitle(document.getTitle());
        response.setStockName(document.getStockName());
        response.setMarket(document.getMarket());
        response.setRiskProfile(document.getRiskProfile());
        response.setTradeStyle(document.getTradeStyle());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setMessageCount(document.getMessages() == null ? 0 : document.getMessages().size());
        return response;
    }

    private AiInvestThreadResponse toResponse(AiInvestThreadDocument document) {
        AiInvestThreadResponse response = new AiInvestThreadResponse();
        response.setThreadId(document.getThreadId());
        response.setTitle(document.getTitle());
        response.setStockName(document.getStockName());
        response.setMarket(document.getMarket());
        response.setRiskProfile(document.getRiskProfile());
        response.setTradeStyle(document.getTradeStyle());
        response.setNotes(document.getNotes());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setMessages(
                (document.getMessages() == null
                        ? List.<AiInvestThreadMessageDocument>of()
                        : document.getMessages()).stream()
                        .map(this::toMessageDto)
                        .collect(Collectors.toList())
        );
        return response;
    }

    private AiInvestThreadMessageDto toMessageDto(AiInvestThreadMessageDocument document) {
        AiInvestThreadMessageDto dto = new AiInvestThreadMessageDto();
        dto.setRole(document.getRole());
        dto.setContent(document.getContent());
        dto.setCreatedAt(document.getCreatedAt());
        dto.setSources(document.getSources());
        return dto;
    }

    private String safeUserId() {
        return userService.getCurrentUser().getUserId().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // -------------------------------------------------------------------------
    // Inner document classes
    // -------------------------------------------------------------------------

    public static class AiInvestThreadDocument {
        private String threadId;
        private String title;
        private String stockName;
        private String market;
        private String riskProfile;
        private String tradeStyle;
        private String notes;
        private Instant createdAt;
        private Instant updatedAt;
        private List<AiInvestThreadMessageDocument> messages;

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

        public String getStockName() {
            return stockName;
        }

        public void setStockName(String stockName) {
            this.stockName = stockName;
        }

        public String getMarket() {
            return market;
        }

        public void setMarket(String market) {
            this.market = market;
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

        public List<AiInvestThreadMessageDocument> getMessages() {
            return messages;
        }

        public void setMessages(List<AiInvestThreadMessageDocument> messages) {
            this.messages = messages;
        }
    }

    public static class AiInvestThreadMessageDocument {
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
