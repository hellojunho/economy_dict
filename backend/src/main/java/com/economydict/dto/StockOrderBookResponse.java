package com.economydict.dto;

import java.util.ArrayList;
import java.util.List;

public class StockOrderBookResponse {
    private List<StockOrderBookLevelResponse> asks = new ArrayList<>();
    private List<StockOrderBookLevelResponse> bids = new ArrayList<>();
    private long totalAskQuantity;
    private long totalBidQuantity;
    private Double expectedPrice;
    private Double expectedChange;
    private Double expectedChangeRate;

    public List<StockOrderBookLevelResponse> getAsks() {
        return asks;
    }

    public void setAsks(List<StockOrderBookLevelResponse> asks) {
        this.asks = asks;
    }

    public List<StockOrderBookLevelResponse> getBids() {
        return bids;
    }

    public void setBids(List<StockOrderBookLevelResponse> bids) {
        this.bids = bids;
    }

    public long getTotalAskQuantity() {
        return totalAskQuantity;
    }

    public void setTotalAskQuantity(long totalAskQuantity) {
        this.totalAskQuantity = totalAskQuantity;
    }

    public long getTotalBidQuantity() {
        return totalBidQuantity;
    }

    public void setTotalBidQuantity(long totalBidQuantity) {
        this.totalBidQuantity = totalBidQuantity;
    }

    public Double getExpectedPrice() {
        return expectedPrice;
    }

    public void setExpectedPrice(Double expectedPrice) {
        this.expectedPrice = expectedPrice;
    }

    public Double getExpectedChange() {
        return expectedChange;
    }

    public void setExpectedChange(Double expectedChange) {
        this.expectedChange = expectedChange;
    }

    public Double getExpectedChangeRate() {
        return expectedChangeRate;
    }

    public void setExpectedChangeRate(Double expectedChangeRate) {
        this.expectedChangeRate = expectedChangeRate;
    }
}
