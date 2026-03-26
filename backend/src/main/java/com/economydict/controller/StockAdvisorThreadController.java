package com.economydict.controller;

import com.economydict.dto.StockAdvisorMessageRequest;
import com.economydict.dto.StockAdvisorThreadCreateRequest;
import com.economydict.dto.StockAdvisorThreadResponse;
import com.economydict.dto.StockAdvisorThreadSummaryResponse;
import com.economydict.service.StockAdvisorThreadService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock-advisor")
public class StockAdvisorThreadController {
    private final StockAdvisorThreadService stockAdvisorThreadService;

    public StockAdvisorThreadController(StockAdvisorThreadService stockAdvisorThreadService) {
        this.stockAdvisorThreadService = stockAdvisorThreadService;
    }

    @GetMapping("/threads")
    public ResponseEntity<List<StockAdvisorThreadSummaryResponse>> listThreads() {
        return ResponseEntity.ok(stockAdvisorThreadService.listThreads());
    }

    @PostMapping("/threads")
    public ResponseEntity<StockAdvisorThreadResponse> createThread(
            @Valid @RequestBody StockAdvisorThreadCreateRequest request
    ) {
        return ResponseEntity.ok(stockAdvisorThreadService.createThread(request));
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<StockAdvisorThreadResponse> getThread(@PathVariable String threadId) {
        return ResponseEntity.ok(stockAdvisorThreadService.getThread(threadId));
    }

    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<StockAdvisorThreadResponse> appendMessage(
            @PathVariable String threadId,
            @Valid @RequestBody StockAdvisorMessageRequest request
    ) {
        return ResponseEntity.ok(stockAdvisorThreadService.appendMessage(threadId, request));
    }

    @DeleteMapping("/threads/{threadId}")
    public ResponseEntity<Void> deleteThread(@PathVariable String threadId) {
        stockAdvisorThreadService.deleteThread(threadId);
        return ResponseEntity.noContent().build();
    }
}
