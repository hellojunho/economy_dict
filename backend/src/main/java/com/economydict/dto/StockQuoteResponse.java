package com.economydict.dto;

public class StockQuoteResponse {
    private String symbol;
    private String name;
    private Double lastPrice;
    private Double change;
    private Double changeRate;
    private Double open;
    private Double high;
    private Double low;
    private Double previousClose;
    private Long volume;
    private Long tradeValue;
    private Long marketCap;
    private Double high52Week;
    private Double low52Week;
    private Double per;
    private Double pbr;
    private Double eps;
    private Double bps;
    private Long listedShares;
    private Long tradeTimestamp;
    private String tradeTimeLabel;
    private String riskLabel;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(Double lastPrice) {
        this.lastPrice = lastPrice;
    }

    public Double getChange() {
        return change;
    }

    public void setChange(Double change) {
        this.change = change;
    }

    public Double getChangeRate() {
        return changeRate;
    }

    public void setChangeRate(Double changeRate) {
        this.changeRate = changeRate;
    }

    public Double getOpen() {
        return open;
    }

    public void setOpen(Double open) {
        this.open = open;
    }

    public Double getHigh() {
        return high;
    }

    public void setHigh(Double high) {
        this.high = high;
    }

    public Double getLow() {
        return low;
    }

    public void setLow(Double low) {
        this.low = low;
    }

    public Double getPreviousClose() {
        return previousClose;
    }

    public void setPreviousClose(Double previousClose) {
        this.previousClose = previousClose;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Long getTradeValue() {
        return tradeValue;
    }

    public void setTradeValue(Long tradeValue) {
        this.tradeValue = tradeValue;
    }

    public Long getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(Long marketCap) {
        this.marketCap = marketCap;
    }

    public Double getHigh52Week() {
        return high52Week;
    }

    public void setHigh52Week(Double high52Week) {
        this.high52Week = high52Week;
    }

    public Double getLow52Week() {
        return low52Week;
    }

    public void setLow52Week(Double low52Week) {
        this.low52Week = low52Week;
    }

    public Double getPer() {
        return per;
    }

    public void setPer(Double per) {
        this.per = per;
    }

    public Double getPbr() {
        return pbr;
    }

    public void setPbr(Double pbr) {
        this.pbr = pbr;
    }

    public Double getEps() {
        return eps;
    }

    public void setEps(Double eps) {
        this.eps = eps;
    }

    public Double getBps() {
        return bps;
    }

    public void setBps(Double bps) {
        this.bps = bps;
    }

    public Long getListedShares() {
        return listedShares;
    }

    public void setListedShares(Long listedShares) {
        this.listedShares = listedShares;
    }

    public Long getTradeTimestamp() {
        return tradeTimestamp;
    }

    public void setTradeTimestamp(Long tradeTimestamp) {
        this.tradeTimestamp = tradeTimestamp;
    }

    public String getTradeTimeLabel() {
        return tradeTimeLabel;
    }

    public void setTradeTimeLabel(String tradeTimeLabel) {
        this.tradeTimeLabel = tradeTimeLabel;
    }

    public String getRiskLabel() {
        return riskLabel;
    }

    public void setRiskLabel(String riskLabel) {
        this.riskLabel = riskLabel;
    }
}
