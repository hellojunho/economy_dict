package com.economydict.service;

import com.economydict.dto.AdminUploadAiModelResponse;
import com.economydict.entity.AppSetting;
import com.economydict.repository.AppSettingRepository;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UploadAiModelSettingsService {
    public static final String UPLOAD_MODEL_SETTING_KEY = "openai.upload.model";

    private static final Set<String> SUPPORTED_MODELS = Set.of(
            "gpt-5.4",
            "gpt-5.4-mini",
            "gpt-5.4-nano",
            "gpt-4.1",
            "gpt-4.1-mini",
            "gpt-4.1-nano"
    );

    private final AppSettingRepository appSettingRepository;
    private final String defaultModel;

    public UploadAiModelSettingsService(AppSettingRepository appSettingRepository,
                                        @Value("${openai.api.model}") String defaultModel) {
        this.appSettingRepository = appSettingRepository;
        this.defaultModel = normalize(defaultModel);
    }

    public AdminUploadAiModelResponse getUploadModelConfig() {
        AdminUploadAiModelResponse response = new AdminUploadAiModelResponse();
        response.setCurrentModel(getCurrentUploadModel());
        response.setDefaultModel(defaultModel);
        return response;
    }

    public String getCurrentUploadModel() {
        return appSettingRepository.findById(UPLOAD_MODEL_SETTING_KEY)
                .map(AppSetting::getSettingValue)
                .map(this::normalize)
                .filter(SUPPORTED_MODELS::contains)
                .orElse(defaultModel);
    }

    public AdminUploadAiModelResponse updateUploadModel(String requestedModel) {
        String normalized = normalize(requestedModel);
        if (!SUPPORTED_MODELS.contains(normalized)) {
            throw new IllegalArgumentException("지원하지 않는 GPT 모델입니다.");
        }

        AppSetting setting = appSettingRepository.findById(UPLOAD_MODEL_SETTING_KEY)
                .orElseGet(AppSetting::new);
        setting.setSettingKey(UPLOAD_MODEL_SETTING_KEY);
        setting.setSettingValue(normalized);
        appSettingRepository.save(setting);

        return getUploadModelConfig();
    }

    private String normalize(String model) {
        return model == null ? "" : model.trim().toLowerCase();
    }
}
