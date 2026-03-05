package com.economydict.controller;

import com.economydict.dto.UserProfileDto;
import com.economydict.entity.User;
import com.economydict.repository.UserRepository;
import com.economydict.service.QuizService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserRepository userRepository;
    private final QuizService quizService;

    public AdminController(UserRepository userRepository, QuizService quizService) {
        this.userRepository = userRepository;
        this.quizService = quizService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserProfileDto>> listUsers() {
        List<UserProfileDto> users = userRepository.findAll().stream()
                .map(this::toProfile)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userId}/quizzes")
    public ResponseEntity<List<String>> userQuizzes(@PathVariable String userId) {
        return ResponseEntity.ok(quizService.getUserQuizzes(userId));
    }

    private UserProfileDto toProfile(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setProfilePicture(user.getProfilePicture());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setActivatedAt(user.getActivatedAt());
        dto.setDeactivatedAt(user.getDeactivatedAt());
        return dto;
    }
}
