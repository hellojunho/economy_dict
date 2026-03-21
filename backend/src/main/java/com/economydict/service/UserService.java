package com.economydict.service;

import com.economydict.dto.UserProfileUpdateRequest;
import com.economydict.dto.UserProfileDto;
import com.economydict.entity.User;
import com.economydict.repository.QuizHistoryRepository;
import com.economydict.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final QuizHistoryRepository quizHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       QuizHistoryRepository quizHistoryRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.quizHistoryRepository = quizHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authentication found");
        }
        String userId = authentication.getName();
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public UserProfileDto getProfile() {
        User user = getCurrentUser();
        UserProfileDto dto = new UserProfileDto();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setProfilePicture(user.getProfilePicture());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setActivatedAt(user.getActivatedAt());
        dto.setDeactivatedAt(user.getDeactivatedAt());
        long totalQuizCount = quizHistoryRepository.countByUser(user);
        long correctCount = quizHistoryRepository.countByUserAndCorrectTrue(user);
        dto.setLearnedWordCount(totalQuizCount);
        dto.setCorrectRate(totalQuizCount == 0 ? 0.0 : (correctCount * 100.0) / totalQuizCount);
        return dto;
    }

    @Transactional
    public UserProfileDto updateProfile(UserProfileUpdateRequest request) {
        User user = getCurrentUser();
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getProfilePicture() != null) {
            user.setProfilePicture(request.getProfilePicture());
        }
        userRepository.save(user);
        return getProfile();
    }
}
