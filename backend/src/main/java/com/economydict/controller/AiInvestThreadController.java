package com.economydict.controller;

import com.economydict.dto.AiInvestMessageRequest;
import com.economydict.dto.AiInvestThreadCreateRequest;
import com.economydict.dto.AiInvestThreadResponse;
import com.economydict.dto.AiInvestThreadSummaryResponse;
import com.economydict.service.AiInvestThreadService;
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
@RequestMapping("/api/ai-invest")
public class AiInvestThreadController {

    private final AiInvestThreadService aiInvestThreadService;

    public AiInvestThreadController(AiInvestThreadService aiInvestThreadService) {
        this.aiInvestThreadService = aiInvestThreadService;
    }

    @GetMapping("/threads")
    public ResponseEntity<List<AiInvestThreadSummaryResponse>> listThreads() {
        return ResponseEntity.ok(aiInvestThreadService.listThreads());
    }

    @PostMapping("/threads")
    public ResponseEntity<AiInvestThreadResponse> createThread(
            @Valid @RequestBody AiInvestThreadCreateRequest request
    ) {
        return ResponseEntity.ok(aiInvestThreadService.createThread(request));
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<AiInvestThreadResponse> getThread(@PathVariable String threadId) {
        return ResponseEntity.ok(aiInvestThreadService.getThread(threadId));
    }

    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<AiInvestThreadResponse> appendMessage(
            @PathVariable String threadId,
            @Valid @RequestBody AiInvestMessageRequest request
    ) {
        return ResponseEntity.ok(aiInvestThreadService.appendMessage(threadId, request));
    }

    @DeleteMapping("/threads/{threadId}")
    public ResponseEntity<Void> deleteThread(@PathVariable String threadId) {
        aiInvestThreadService.deleteThread(threadId);
        return ResponseEntity.noContent().build();
    }
}
