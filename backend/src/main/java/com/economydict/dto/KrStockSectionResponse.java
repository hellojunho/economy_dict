package com.economydict.dto;

import java.util.ArrayList;
import java.util.List;

public class KrStockSectionResponse {
    private String title;
    private List<KrStockMetricResponse> rows = new ArrayList<>();

    public KrStockSectionResponse() {
    }

    public KrStockSectionResponse(String title, List<KrStockMetricResponse> rows) {
        this.title = title;
        this.rows = rows;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<KrStockMetricResponse> getRows() {
        return rows;
    }

    public void setRows(List<KrStockMetricResponse> rows) {
        this.rows = rows;
    }
}
