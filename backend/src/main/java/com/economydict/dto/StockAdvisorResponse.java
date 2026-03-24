package com.economydict.dto;

import java.time.Instant;
import java.util.List;

public class StockAdvisorResponse {
    private String symbol;
    private String riskProfile;
    private String tradeStyle;
    private Instant generatedAt;
    private String content;
    private List<StockAdvisorSourceResponse> sources;

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

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<StockAdvisorSourceResponse> getSources() {
        return sources;
    }

    public void setSources(List<StockAdvisorSourceResponse> sources) {
        this.sources = sources;
    }
}
