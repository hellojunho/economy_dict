package com.economydict.dto;

import java.time.Instant;
import java.util.List;

public class AiInvestThreadResponse {

    private String threadId;
    private String title;
    private String stockName;
    private String market;
    private String riskProfile;
    private String tradeStyle;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private List<AiInvestThreadMessageDto> messages;

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

    public List<AiInvestThreadMessageDto> getMessages() {
        return messages;
    }

    public void setMessages(List<AiInvestThreadMessageDto> messages) {
        this.messages = messages;
    }
}
