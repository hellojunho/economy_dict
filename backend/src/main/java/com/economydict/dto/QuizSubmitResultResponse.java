package com.economydict.dto;

public class QuizSubmitResultResponse {
    private int totalCount;
    private int correctCount;

    public QuizSubmitResultResponse(int totalCount, int correctCount) {
        this.totalCount = totalCount;
        this.correctCount = correctCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getCorrectCount() {
        return correctCount;
    }
}
