package com.economydict.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class StockSnapshotResponse {
    private String provider;
    private String symbol;
    private String name;
    private String market;
    private int liveRefreshIntervalSeconds;
    private Instant fetchedAt;
    private StockQuoteResponse quote;
    private StockOrderBookResponse orderBook;
    private List<StockCandleResponse> intradayCandles = new ArrayList<>();
    private List<StockCandleResponse> dailyCandles = new ArrayList<>();
    private List<StockSectionResponse> sections = new ArrayList<>();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public int getLiveRefreshIntervalSeconds() {
        return liveRefreshIntervalSeconds;
    }

    public void setLiveRefreshIntervalSeconds(int liveRefreshIntervalSeconds) {
        this.liveRefreshIntervalSeconds = liveRefreshIntervalSeconds;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public StockQuoteResponse getQuote() {
        return quote;
    }

    public void setQuote(StockQuoteResponse quote) {
        this.quote = quote;
    }

    public StockOrderBookResponse getOrderBook() {
        return orderBook;
    }

    public void setOrderBook(StockOrderBookResponse orderBook) {
        this.orderBook = orderBook;
    }

    public List<StockCandleResponse> getIntradayCandles() {
        return intradayCandles;
    }

    public void setIntradayCandles(List<StockCandleResponse> intradayCandles) {
        this.intradayCandles = intradayCandles;
    }

    public List<StockCandleResponse> getDailyCandles() {
        return dailyCandles;
    }

    public void setDailyCandles(List<StockCandleResponse> dailyCandles) {
        this.dailyCandles = dailyCandles;
    }

    public List<StockSectionResponse> getSections() {
        return sections;
    }

    public void setSections(List<StockSectionResponse> sections) {
        this.sections = sections;
    }
}
