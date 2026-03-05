package com.economydict.controller;

import com.economydict.dto.UserProfileDto;
import com.economydict.service.QuizService;
import com.economydict.service.UserService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final QuizService quizService;

    public UserController(UserService userService, QuizService quizService) {
        this.userService = userService;
        this.quizService = quizService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> me() {
        return ResponseEntity.ok(userService.getProfile());
    }

    @GetMapping("/me/quizzes")
    public ResponseEntity<List<String>> myQuizzes() {
        return ResponseEntity.ok(quizService.getMyQuizzes());
    }
}
