package com.economydict.dto;

public class KrStockOrderBookLevelResponse {
    private double price;
    private long quantity;

    public KrStockOrderBookLevelResponse() {
    }

    public KrStockOrderBookLevelResponse(double price, long quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }
}
