package com.economydict.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class QuizSubmitAnswersRequest {
    @NotEmpty
    private List<QuizAnswerItemRequest> answers;

    public List<QuizAnswerItemRequest> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuizAnswerItemRequest> answers) {
        this.answers = answers;
    }
}
