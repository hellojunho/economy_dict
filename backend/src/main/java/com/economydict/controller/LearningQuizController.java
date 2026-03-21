package com.economydict.controller;

import com.economydict.dto.DailyQuizResponse;
import com.economydict.dto.IncorrectQuizQuestionResponse;
import com.economydict.dto.QuizSubmitAnswersRequest;
import com.economydict.dto.QuizSubmitResultResponse;
import com.economydict.dto.TopIncorrectWordResponse;
import com.economydict.service.LearningQuizService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quizzes")
public class LearningQuizController {
    private final LearningQuizService learningQuizService;

    public LearningQuizController(LearningQuizService learningQuizService) {
        this.learningQuizService = learningQuizService;
    }

    @GetMapping("/daily")
    public ResponseEntity<DailyQuizResponse> daily() {
        return ResponseEntity.ok(learningQuizService.getDailyQuiz());
    }

    @PostMapping("/submit")
    public ResponseEntity<QuizSubmitResultResponse> submit(@Valid @RequestBody QuizSubmitAnswersRequest request) {
        throw new IllegalArgumentException("퀴즈 제출은 /api/quizzes/{quizId}/submit 경로를 사용하세요.");
    }

    @GetMapping("/incorrect")
    public ResponseEntity<List<IncorrectQuizQuestionResponse>> incorrect() {
        return ResponseEntity.ok(learningQuizService.getIncorrectQuestions());
    }

    @GetMapping("/top-100")
    public ResponseEntity<List<TopIncorrectWordResponse>> top100() {
        return ResponseEntity.ok(learningQuizService.getTop100());
    }
}
