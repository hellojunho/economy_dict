package com.economydict.dto;

import java.util.ArrayList;
import java.util.List;

public class KrStockOrderBookResponse {
    private List<KrStockOrderBookLevelResponse> asks = new ArrayList<>();
    private List<KrStockOrderBookLevelResponse> bids = new ArrayList<>();
    private Long totalAskQuantity;
    private Long totalBidQuantity;
    private String baseTime;

    public List<KrStockOrderBookLevelResponse> getAsks() {
        return asks;
    }

    public void setAsks(List<KrStockOrderBookLevelResponse> asks) {
        this.asks = asks;
    }

    public List<KrStockOrderBookLevelResponse> getBids() {
        return bids;
    }

    public void setBids(List<KrStockOrderBookLevelResponse> bids) {
        this.bids = bids;
    }

    public Long getTotalAskQuantity() {
        return totalAskQuantity;
    }

    public void setTotalAskQuantity(Long totalAskQuantity) {
        this.totalAskQuantity = totalAskQuantity;
    }

    public Long getTotalBidQuantity() {
        return totalBidQuantity;
    }

    public void setTotalBidQuantity(Long totalBidQuantity) {
        this.totalBidQuantity = totalBidQuantity;
    }

    public String getBaseTime() {
        return baseTime;
    }

    public void setBaseTime(String baseTime) {
        this.baseTime = baseTime;
    }
}
