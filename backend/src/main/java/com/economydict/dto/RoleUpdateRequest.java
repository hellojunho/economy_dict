package com.economydict.dto;

import com.economydict.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public class RoleUpdateRequest {
    @NotNull
    private UserRole role;

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
