package com.economydict.dto;

public class AdminUploadAiModelResponse {
    private String currentModel;
    private String defaultModel;

    public String getCurrentModel() {
        return currentModel;
    }

    public void setCurrentModel(String currentModel) {
        this.currentModel = currentModel;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }
}
