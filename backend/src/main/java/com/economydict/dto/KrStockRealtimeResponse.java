package com.economydict.dto;

import java.time.Instant;

public class KrStockRealtimeResponse {
    private String symbol;
    private Instant fetchedAt;
    private KrStockQuoteResponse quote;
    private KrStockOrderBookResponse orderBook;

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
}
