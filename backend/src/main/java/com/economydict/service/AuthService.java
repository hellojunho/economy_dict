package com.economydict.service;

import com.economydict.config.JwtTokenProvider;
import com.economydict.dto.AuthLoginRequest;
import com.economydict.dto.AuthSignupRequest;
import com.economydict.entity.RevokedToken;
import com.economydict.entity.User;
import com.economydict.entity.UserLogAction;
import com.economydict.entity.UserStatus;
import com.economydict.repository.RevokedTokenRepository;
import com.economydict.repository.UserRepository;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RevokedTokenRepository revokedTokenRepository;
    private final UserLogService userLogService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       RevokedTokenRepository revokedTokenRepository,
                       UserLogService userLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.revokedTokenRepository = revokedTokenRepository;
        this.userLogService = userLogService;
    }

    @Transactional
    public User register(AuthSignupRequest request) {
        if (userRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("User ID already exists");
        }
        User user = new User();
        user.setUserId(request.getUserId());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setProfilePicture(request.getProfilePicture());
        user.setStatus(UserStatus.ACTIVE);
        user.setActivatedAt(Instant.now());
        User saved = userRepository.save(user);
        userLogService.log(saved, UserLogAction.SIGNUP, "User signed up");
        return saved;
    }

    public User login(AuthLoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword())
        );
        User user = userRepository.findByUserId(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("User is deactivated");
        }
        userLogService.log(user, UserLogAction.LOGIN, "User logged in");
        return user;
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        if (revokedTokenRepository.existsByToken(token)) {
            return;
        }
        RevokedToken revokedToken = new RevokedToken();
        revokedToken.setToken(token);
        revokedToken.setExpiresAt(tokenProvider.getExpiration(token));
        revokedTokenRepository.save(revokedToken);
    }

    @Transactional
    public void deactivate(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus(UserStatus.DEACTIVATED);
        user.setDeactivatedAt(Instant.now());
        userRepository.save(user);
        userLogService.log(user, UserLogAction.WITHDRAW, "User withdrew");
    }

    public String createAccessToken(User user) {
        return tokenProvider.createToken(user);
    }
}
