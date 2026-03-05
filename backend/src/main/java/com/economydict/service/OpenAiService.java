package com.economydict.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OpenAiService {
    private final WebClient webClient;
    private final String model;

    public OpenAiService(@Value("${openai.api.base-url}") String baseUrl,
                         @Value("${openai.api.key}") String apiKey,
                         @Value("${openai.api.model}") String model) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.model = model;
    }

    public String chat(String message) {
        ChatRequest request = new ChatRequest(model, List.of(new ChatMessage("user", message)));
        ChatResponse response = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block();
        if (response == null || response.choices == null || response.choices.isEmpty()) {
            return "";
        }
        return response.choices.get(0).message.content;
    }

    private static class ChatRequest {
        private String model;
        private List<ChatMessage> messages;

        public ChatRequest(String model, List<ChatMessage> messages) {
            this.model = model;
            this.messages = messages;
        }

        public String getModel() {
            return model;
        }

        public List<ChatMessage> getMessages() {
            return messages;
        }
    }

    private static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }

    private static class ChatResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }
    }

    private static class Choice {
        private ChatMessage message;

        public ChatMessage getMessage() {
            return message;
        }
    }
}
