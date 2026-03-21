package com.economydict.dto;

import java.time.Instant;
import java.time.LocalDate;

public class DailyUserStatResponse {
    private LocalDate targetDate;
    private int newUsersCount;
    private int loginCount;
    private int activeUsersCount;
    private Instant createdAt;

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public int getNewUsersCount() {
        return newUsersCount;
    }

    public void setNewUsersCount(int newUsersCount) {
        this.newUsersCount = newUsersCount;
    }

    public int getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }

    public int getActiveUsersCount() {
        return activeUsersCount;
    }

    public void setActiveUsersCount(int activeUsersCount) {
        this.activeUsersCount = activeUsersCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
