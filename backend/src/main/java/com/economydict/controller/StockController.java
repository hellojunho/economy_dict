package com.economydict.controller;

import com.economydict.dto.StockRealtimeResponse;
import com.economydict.dto.StockSnapshotResponse;
import com.economydict.service.KisStockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks/korea")
public class StockController {
    private final KisStockService kisStockService;

    public StockController(KisStockService kisStockService) {
        this.kisStockService = kisStockService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<StockSnapshotResponse> snapshot(@PathVariable String symbol) {
        return ResponseEntity.ok(kisStockService.getSnapshot(symbol));
    }

    @GetMapping("/{symbol}/realtime")
    public ResponseEntity<StockRealtimeResponse> realtime(@PathVariable String symbol) {
        return ResponseEntity.ok(kisStockService.getRealtime(symbol));
    }
}
