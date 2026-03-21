package com.economydict.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AdminQuizDto {
    private Long id;
    private String quizId;
    private String title;
    private int questionCount;
    private int participantCount;
    private Instant createdAt;
    private List<AdminQuestionDto> questions = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<AdminQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<AdminQuestionDto> questions) {
        this.questions = questions;
    }
}
