package com.economydict.dto;

import java.util.List;

public class DailyQuizResponse {
    private List<DailyQuizQuestionResponse> questions;

    public List<DailyQuizQuestionResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<DailyQuizQuestionResponse> questions) {
        this.questions = questions;
    }
}
