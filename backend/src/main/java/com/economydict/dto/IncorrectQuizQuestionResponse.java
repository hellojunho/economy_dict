package com.economydict.dto;

import java.util.ArrayList;
import java.util.List;

public class IncorrectQuizQuestionResponse {
    private Long questionId;
    private String quizId;
    private String quizTitle;
    private String questionText;
    private List<DailyQuizOptionResponse> options = new ArrayList<>();

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getQuizId() {
        return quizId;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<DailyQuizOptionResponse> getOptions() {
        return options;
    }

    public void setOptions(List<DailyQuizOptionResponse> options) {
        this.options = options;
    }
}
