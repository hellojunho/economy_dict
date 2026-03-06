package com.economydict.dto;

import com.economydict.entity.UserRole;
import com.economydict.entity.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminUserRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String userId;

    @NotBlank
    @Size(min = 2, max = 100)
    private String username;

    private String password;

    private String email;

    private String profilePicture;

    private UserRole role;

    private UserStatus status;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
