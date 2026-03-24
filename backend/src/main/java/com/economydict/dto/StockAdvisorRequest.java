package com.economydict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StockAdvisorRequest {
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[0-9A-Za-z:_./!\\-]+$", message = "symbol must be a valid TradingView symbol.")
    private String symbol;

    @NotBlank
    @Size(max = 30)
    private String riskProfile;

    @NotBlank
    @Size(max = 30)
    private String tradeStyle;

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
}
