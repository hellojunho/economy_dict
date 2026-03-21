package com.economydict.dto;

import com.economydict.entity.UserRole;

public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private UserRole role;

    public AuthResponse(String accessToken, UserRole role) {
        this.accessToken = accessToken;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public UserRole getRole() {
        return role;
    }
}
