package com.economydict.controller;

import com.economydict.dto.ChatMessageRequest;
import com.economydict.dto.ChatThreadCreateRequest;
import com.economydict.dto.ChatThreadResponse;
import com.economydict.dto.ChatThreadSummaryResponse;
import com.economydict.service.ChatThreadService;
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
@RequestMapping("/api/chats")
public class ChatController {
    private final ChatThreadService chatThreadService;

    public ChatController(ChatThreadService chatThreadService) {
        this.chatThreadService = chatThreadService;
    }

    @GetMapping
    public ResponseEntity<List<ChatThreadSummaryResponse>> list() {
        return ResponseEntity.ok(chatThreadService.listThreads());
    }

    @PostMapping
    public ResponseEntity<ChatThreadResponse> create(@RequestBody(required = false) ChatThreadCreateRequest request) {
        return ResponseEntity.ok(chatThreadService.createThread(request == null ? new ChatThreadCreateRequest() : request));
    }

    @GetMapping("/{threadId}")
    public ResponseEntity<ChatThreadResponse> detail(@PathVariable String threadId) {
        return ResponseEntity.ok(chatThreadService.getThread(threadId));
    }

    @PostMapping("/{threadId}/messages")
    public ResponseEntity<ChatThreadResponse> append(@PathVariable String threadId, @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatThreadService.appendMessage(threadId, request));
    }

    @DeleteMapping("/{threadId}")
    public ResponseEntity<Void> delete(@PathVariable String threadId) {
        chatThreadService.deleteThread(threadId);
        return ResponseEntity.noContent().build();
    }
}
