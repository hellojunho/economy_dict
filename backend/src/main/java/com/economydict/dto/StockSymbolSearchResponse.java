package com.economydict.dto;

public class StockSymbolSearchResponse {
    private String symbol;
    private String description;
    private String exchange;
    private String type;
    private String country;
    private boolean directViewOnly;
    private String directViewOnlyMessage;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isDirectViewOnly() {
        return directViewOnly;
    }

    public void setDirectViewOnly(boolean directViewOnly) {
        this.directViewOnly = directViewOnly;
    }

    public String getDirectViewOnlyMessage() {
        return directViewOnlyMessage;
    }

    public void setDirectViewOnlyMessage(String directViewOnlyMessage) {
        this.directViewOnlyMessage = directViewOnlyMessage;
    }
}
