package com.economydict.dto;

import java.time.Instant;
import java.util.List;

public class AiInvestThreadMessageDto {

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
