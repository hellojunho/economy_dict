package com.economydict.controller;

import com.economydict.dto.KrStockRealtimeResponse;
import com.economydict.dto.KrStockSnapshotResponse;
import com.economydict.service.KiwoomKrStockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kr-stocks")
public class KrStockController {
    private final KiwoomKrStockService kiwoomKrStockService;

    public KrStockController(KiwoomKrStockService kiwoomKrStockService) {
        this.kiwoomKrStockService = kiwoomKrStockService;
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<KrStockSnapshotResponse> snapshot(@PathVariable String symbol) {
        return ResponseEntity.ok(kiwoomKrStockService.getSnapshot(symbol));
    }

    @GetMapping("/{symbol}/realtime")
    public ResponseEntity<KrStockRealtimeResponse> realtime(@PathVariable String symbol) {
        return ResponseEntity.ok(kiwoomKrStockService.getRealtime(symbol));
    }
}
