package com.economydict.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminQuestionDto {
    private Long id;
    private Long quizId;
    private String questionText;
    private long attemptedUsers;
    private long correctUsers;
    private double correctRate;
    private List<String> participants = new ArrayList<>();
    private List<String> correctParticipants = new ArrayList<>();
    private List<AdminOptionDto> options = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public long getAttemptedUsers() {
        return attemptedUsers;
    }

    public void setAttemptedUsers(long attemptedUsers) {
        this.attemptedUsers = attemptedUsers;
    }

    public long getCorrectUsers() {
        return correctUsers;
    }

    public void setCorrectUsers(long correctUsers) {
        this.correctUsers = correctUsers;
    }

    public double getCorrectRate() {
        return correctRate;
    }

    public void setCorrectRate(double correctRate) {
        this.correctRate = correctRate;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public List<String> getCorrectParticipants() {
        return correctParticipants;
    }

    public void setCorrectParticipants(List<String> correctParticipants) {
        this.correctParticipants = correctParticipants;
    }

    public List<AdminOptionDto> getOptions() {
        return options;
    }

    public void setOptions(List<AdminOptionDto> options) {
        this.options = options;
    }
}
