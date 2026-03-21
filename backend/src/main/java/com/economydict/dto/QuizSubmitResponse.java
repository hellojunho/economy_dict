package com.economydict.dto;

public class QuizSubmitResponse {
    private int totalQuestions;
    private long correctCount;
    private boolean completed;
    private boolean submittedCorrect;
    private long recordedCorrectCount;
    private long recordedIncorrectCount;

    public QuizSubmitResponse(int totalQuestions,
                              long correctCount,
                              boolean completed,
                              boolean submittedCorrect,
                              long recordedCorrectCount,
                              long recordedIncorrectCount) {
        this.totalQuestions = totalQuestions;
        this.correctCount = correctCount;
        this.completed = completed;
        this.submittedCorrect = submittedCorrect;
        this.recordedCorrectCount = recordedCorrectCount;
        this.recordedIncorrectCount = recordedIncorrectCount;
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

    public boolean isSubmittedCorrect() {
        return submittedCorrect;
    }

    public long getRecordedCorrectCount() {
        return recordedCorrectCount;
    }

    public long getRecordedIncorrectCount() {
        return recordedIncorrectCount;
    }
}
