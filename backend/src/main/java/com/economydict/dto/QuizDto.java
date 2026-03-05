package com.economydict.dto;

import java.util.ArrayList;
import java.util.List;

public class QuizDto {
    private String quizId;
    private String title;
    private List<QuizQuestionDto> questions = new ArrayList<>();

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

    public List<QuizQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestionDto> questions) {
        this.questions = questions;
    }
}
