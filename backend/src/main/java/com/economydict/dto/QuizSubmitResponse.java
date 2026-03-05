package com.economydict.dto;

public class QuizSubmitResponse {
    private int totalQuestions;
    private long correctCount;
    private boolean completed;

    public QuizSubmitResponse(int totalQuestions, long correctCount, boolean completed) {
        this.totalQuestions = totalQuestions;
        this.correctCount = correctCount;
        this.completed = completed;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public long getCorrectCount() {
        return correctCount;
    }

    public boolean isCompleted() {
        return completed;
    }
}
