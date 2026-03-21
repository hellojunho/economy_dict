package com.economydict.service;

import com.economydict.dto.ChatMessageRequest;
import com.economydict.dto.ChatThreadCreateRequest;
import com.economydict.dto.ChatThreadMessageDto;
import com.economydict.dto.ChatThreadResponse;
import com.economydict.dto.ChatThreadSummaryResponse;
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
public class ChatThreadService {
    private final UserService userService;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final Path storageRoot;

    public ChatThreadService(UserService userService,
                             OpenAiService openAiService,
                             ObjectMapper objectMapper,
                             @Value("${app.chat.storage-path:/app/backend/chats}") String storagePath) {
        this.userService = userService;
        this.openAiService = openAiService;
        this.objectMapper = objectMapper;
        this.storageRoot = Paths.get(storagePath);
    }

    public List<ChatThreadSummaryResponse> listThreads() {
        Path userDirectory = ensureUserDirectory();
        try (Stream<Path> paths = Files.list(userDirectory)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(this::readThread)
                    .sorted(Comparator.comparing(ChatThreadDocument::getUpdatedAt).reversed())
                    .map(this::toSummary)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read chat thread list", ex);
        }
    }

    public ChatThreadResponse createThread(ChatThreadCreateRequest request) {
        ChatThreadDocument document = new ChatThreadDocument();
        document.setThreadId(UUID.randomUUID().toString());
        document.setTitle(request == null || request.getTitle() == null || request.getTitle().isBlank()
                ? "새 대화"
                : request.getTitle().trim());
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(document.getCreatedAt());
        document.setMessages(new ArrayList<>());
        writeThread(document);
        return toResponse(document);
    }

    public ChatThreadResponse getThread(String threadId) {
        return toResponse(readThread(resolveThreadPath(threadId)));
    }

    @Transactional
    public ChatThreadResponse appendMessage(String threadId, ChatMessageRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Chat message is required");
        }

        ChatThreadDocument document = readThread(resolveThreadPath(threadId));
        ChatThreadMessageDocument userMessage = new ChatThreadMessageDocument();
        userMessage.setRole("user");
        userMessage.setContent(request.getMessage().trim());
        userMessage.setCreatedAt(Instant.now());
        document.getMessages().add(userMessage);

        if ("새 대화".equals(document.getTitle()) || document.getTitle() == null || document.getTitle().isBlank()) {
            document.setTitle(summarizeTitle(request.getMessage()));
        }

        String reply = openAiService.chat(document.getMessages().stream()
                .map(message -> new OpenAiService.ChatTurn(message.getRole(), message.getContent()))
                .collect(Collectors.toList()));

        ChatThreadMessageDocument assistantMessage = new ChatThreadMessageDocument();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(reply);
        assistantMessage.setCreatedAt(Instant.now());
        document.getMessages().add(assistantMessage);
        document.setUpdatedAt(Instant.now());
        writeThread(document);
        return toResponse(document);
    }

    public void deleteThread(String threadId) {
        try {
            Files.deleteIfExists(resolveThreadPath(threadId));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to delete chat thread", ex);
        }
    }

    private Path ensureUserDirectory() {
        try {
            Files.createDirectories(storageRoot);
            Path userDirectory = storageRoot.resolve(safeUserId());
            Files.createDirectories(userDirectory);
            return userDirectory;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare chat storage", ex);
        }
    }

    private Path resolveThreadPath(String threadId) {
        return ensureUserDirectory().resolve(threadId + ".json");
    }

    private ChatThreadDocument readThread(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), ChatThreadDocument.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Chat thread not found");
        }
    }

    private void writeThread(ChatThreadDocument document) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(resolveThreadPath(document.getThreadId()).toFile(), document);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist chat thread", ex);
        }
    }

    private ChatThreadSummaryResponse toSummary(ChatThreadDocument document) {
        ChatThreadSummaryResponse response = new ChatThreadSummaryResponse();
        response.setThreadId(document.getThreadId());
        response.setTitle(document.getTitle());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setMessageCount(document.getMessages() == null ? 0 : document.getMessages().size());
        return response;
    }

    private ChatThreadResponse toResponse(ChatThreadDocument document) {
        ChatThreadResponse response = new ChatThreadResponse();
        response.setThreadId(document.getThreadId());
        response.setTitle(document.getTitle());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setMessages((document.getMessages() == null ? List.<ChatThreadMessageDocument>of() : document.getMessages()).stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList()));
        return response;
    }

    private ChatThreadMessageDto toMessageDto(ChatThreadMessageDocument document) {
        ChatThreadMessageDto dto = new ChatThreadMessageDto();
        dto.setRole(document.getRole());
        dto.setContent(document.getContent());
        dto.setCreatedAt(document.getCreatedAt());
        return dto;
    }

    private String summarizeTitle(String message) {
        String trimmed = message.trim().replaceAll("\\s+", " ");
        return trimmed.length() <= 28 ? trimmed : trimmed.substring(0, 28) + "...";
    }

    private String safeUserId() {
        return userService.getCurrentUser().getUserId().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public static class ChatThreadDocument {
        private String threadId;
        private String title;
        private Instant createdAt;
        private Instant updatedAt;
        private List<ChatThreadMessageDocument> messages;

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

        public List<ChatThreadMessageDocument> getMessages() {
            return messages;
        }

        public void setMessages(List<ChatThreadMessageDocument> messages) {
            this.messages = messages;
        }
    }

    public static class ChatThreadMessageDocument {
        private String role;
        private String content;
        private Instant createdAt;

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
    }
}
