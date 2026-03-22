package com.economydict.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class KrStockSnapshotResponse {
    private String provider;
    private String market;
    private String symbol;
    private String name;
    private Instant fetchedAt;
    private int liveRefreshIntervalSeconds;
    private KrStockQuoteResponse quote;
    private KrStockOrderBookResponse orderBook;
    private List<KrStockCandleResponse> intradayCandles = new ArrayList<>();
    private List<KrStockCandleResponse> dailyCandles = new ArrayList<>();
    private List<KrStockSectionResponse> sections = new ArrayList<>();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

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

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public int getLiveRefreshIntervalSeconds() {
        return liveRefreshIntervalSeconds;
    }

    public void setLiveRefreshIntervalSeconds(int liveRefreshIntervalSeconds) {
        this.liveRefreshIntervalSeconds = liveRefreshIntervalSeconds;
    }

    public KrStockQuoteResponse getQuote() {
        return quote;
    }

    public void setQuote(KrStockQuoteResponse quote) {
        this.quote = quote;
    }

    public KrStockOrderBookResponse getOrderBook() {
        return orderBook;
    }

    public void setOrderBook(KrStockOrderBookResponse orderBook) {
        this.orderBook = orderBook;
    }

    public List<KrStockCandleResponse> getIntradayCandles() {
        return intradayCandles;
    }

    public void setIntradayCandles(List<KrStockCandleResponse> intradayCandles) {
        this.intradayCandles = intradayCandles;
    }

    public List<KrStockCandleResponse> getDailyCandles() {
        return dailyCandles;
    }

    public void setDailyCandles(List<KrStockCandleResponse> dailyCandles) {
        this.dailyCandles = dailyCandles;
    }

    public List<KrStockSectionResponse> getSections() {
        return sections;
    }

    public void setSections(List<KrStockSectionResponse> sections) {
        this.sections = sections;
    }
}
