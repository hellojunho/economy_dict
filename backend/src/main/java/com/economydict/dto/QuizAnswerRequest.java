package com.economydict.dto;

import jakarta.validation.constraints.NotNull;

public class QuizAnswerRequest {
    @NotNull
    private Long questionId;

    @NotNull
    private Long selectedOptionId;

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(Long selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }
}
