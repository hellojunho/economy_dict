package com.economydict.dto;

import java.time.Instant;
import java.util.List;

public class StockAdvisorThreadResponse {
    private String threadId;
    private String title;
    private String symbol;
    private String riskProfile;
    private String tradeStyle;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private List<StockAdvisorThreadMessageDto> messages;

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

    public List<StockAdvisorThreadMessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<StockAdvisorThreadMessageDto> messages) {
        this.messages = messages;
    }
}
