package com.economydict.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class QuizSubmitRequest {
    @NotEmpty
    private List<QuizAnswerRequest> answers = new ArrayList<>();

    public List<QuizAnswerRequest> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuizAnswerRequest> answers) {
        this.answers = answers;
    }
}
