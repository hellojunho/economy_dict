package com.economydict.dto;

import java.util.ArrayList;
import java.util.List;

public class DailyQuizResponse {
    private String quizId;
    private String title;
    private List<DailyQuizQuestionResponse> questions;
    private List<Long> solvedQuestionIds = new ArrayList<>();
    private long recordedCorrectCount;
    private long recordedIncorrectCount;

    public String getQuizId() {
        return quizId;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<DailyQuizQuestionResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<DailyQuizQuestionResponse> questions) {
        this.questions = questions;
    }

    public List<Long> getSolvedQuestionIds() {
        return solvedQuestionIds;
    }

    public void setSolvedQuestionIds(List<Long> solvedQuestionIds) {
        this.solvedQuestionIds = solvedQuestionIds;
    }

    public long getRecordedCorrectCount() {
        return recordedCorrectCount;
    }

    public void setRecordedCorrectCount(long recordedCorrectCount) {
        this.recordedCorrectCount = recordedCorrectCount;
    }

    public long getRecordedIncorrectCount() {
        return recordedIncorrectCount;
    }

    public void setRecordedIncorrectCount(long recordedIncorrectCount) {
        this.recordedIncorrectCount = recordedIncorrectCount;
    }
}
