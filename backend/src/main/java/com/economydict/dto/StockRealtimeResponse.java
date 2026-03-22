package com.economydict.dto;

import java.time.Instant;

public class StockRealtimeResponse {
    private String symbol;
    private Instant fetchedAt;
    private StockQuoteResponse quote;
    private StockOrderBookResponse orderBook;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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
}
