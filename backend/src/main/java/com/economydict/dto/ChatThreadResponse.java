package com.economydict.dto;

import java.time.Instant;
import java.util.List;

public class ChatThreadResponse {
    private String threadId;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ChatThreadMessageDto> messages;

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

    public List<ChatThreadMessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatThreadMessageDto> messages) {
        this.messages = messages;
    }
}
