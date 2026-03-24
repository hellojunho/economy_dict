package com.economydict.controller;

import com.economydict.dto.StockAdvisorRequest;
import com.economydict.dto.StockAdvisorResponse;
import com.economydict.dto.StockSymbolSearchResponse;
import com.economydict.service.StockAdvisorService;
import com.economydict.service.TradingViewSymbolService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockAdvisorController {
    private final StockAdvisorService stockAdvisorService;
    private final TradingViewSymbolService tradingViewSymbolService;

    public StockAdvisorController(
            StockAdvisorService stockAdvisorService,
            TradingViewSymbolService tradingViewSymbolService
    ) {
        this.stockAdvisorService = stockAdvisorService;
        this.tradingViewSymbolService = tradingViewSymbolService;
    }

    @GetMapping("/symbols")
    public ResponseEntity<List<StockSymbolSearchResponse>> symbols(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(tradingViewSymbolService.search(query));
    }

    @PostMapping("/advisor")
    public ResponseEntity<StockAdvisorResponse> analyze(@Valid @RequestBody StockAdvisorRequest request) {
        return ResponseEntity.ok(stockAdvisorService.analyze(request));
    }
}
