package com.economydict.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminUploadAiModelRequest {
    @NotBlank
    private String model;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
