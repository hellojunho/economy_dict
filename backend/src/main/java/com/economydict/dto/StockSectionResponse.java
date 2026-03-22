package com.economydict.dto;

import java.util.ArrayList;
import java.util.List;

public class StockSectionResponse {
    private String title;
    private List<StockMetricResponse> rows = new ArrayList<>();

    public StockSectionResponse() {
    }

    public StockSectionResponse(String title, List<StockMetricResponse> rows) {
        this.title = title;
        this.rows = rows;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<StockMetricResponse> getRows() {
        return rows;
    }

    public void setRows(List<StockMetricResponse> rows) {
        this.rows = rows;
    }
}
