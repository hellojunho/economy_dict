package com.economydict.controller;

import com.economydict.dto.QuizDto;
import com.economydict.dto.QuizSubmitRequest;
import com.economydict.dto.QuizSubmitResponse;
import com.economydict.service.QuizService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {
    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public ResponseEntity<List<QuizDto>> list() {
        return ResponseEntity.ok(quizService.getAllQuizzes());
    }

    @GetMapping("/{quizId}")
    public ResponseEntity<QuizDto> getQuiz(@PathVariable String quizId) {
        return ResponseEntity.ok(quizService.getQuiz(quizId));
    }

    @PostMapping("/{quizId}/submit")
    public ResponseEntity<QuizSubmitResponse> submit(@PathVariable String quizId,
                                                     @Valid @RequestBody QuizSubmitRequest request) {
        return ResponseEntity.ok(quizService.submit(quizId, request));
    }

    @GetMapping("/{quizId}/participants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> participants(@PathVariable String quizId) {
        return ResponseEntity.ok(quizService.getParticipants(quizId));
    }
}
