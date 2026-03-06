package com.economydict.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OpenAiService {
    private final WebClient webClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiService(@Value("${openai.api.base-url}") String baseUrl,
                         @Value("${openai.api.key}") String apiKey,
                         @Value("${openai.api.model}") String model) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.model = model;
        this.objectMapper = new ObjectMapper();
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

    public List<ExtractedTerm> extractDictionaryTerms(String documentText) {
        String prompt = """
                You are a precise extractor. Read the document text and extract economic terms.
                Return ONLY valid JSON array with objects: word, meaning, englishWord, englishMeaning.
                If englishWord or englishMeaning is not available, set null.
                Keep words concise and avoid duplicates.
                Document:
                """ + documentText;
        ChatRequest request = new ChatRequest(model, List.of(
                new ChatMessage("system", "You are an expert in economics glossary extraction."),
                new ChatMessage("user", prompt)
        ));
        ChatResponse response = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block();
        if (response == null || response.choices == null || response.choices.isEmpty()) {
            return Collections.emptyList();
        }
        String content = response.choices.get(0).message.content;
        try {
            return objectMapper.readValue(content, new TypeReference<List<ExtractedTerm>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static class ExtractedTerm {
        private String word;
        private String meaning;
        private String englishWord;
        private String englishMeaning;

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public String getMeaning() {
            return meaning;
        }

        public void setMeaning(String meaning) {
            this.meaning = meaning;
        }

        public String getEnglishWord() {
            return englishWord;
        }

        public void setEnglishWord(String englishWord) {
            this.englishWord = englishWord;
        }

        public String getEnglishMeaning() {
            return englishMeaning;
        }

        public void setEnglishMeaning(String englishMeaning) {
            this.englishMeaning = englishMeaning;
        }
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
