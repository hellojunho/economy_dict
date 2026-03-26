package com.economydict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiInvestThreadCreateRequest {

    @NotBlank
    @Size(max = 60)
    private String stockName;

    @Size(max = 10)
    private String market;

    @NotBlank
    @Size(max = 30)
    private String riskProfile;

    @NotBlank
    @Size(max = 30)
    private String tradeStyle;

    @Size(max = 500)
    private String notes;

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
}
