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
        return chat(List.of(new ChatTurn("user", message)));
    }

    public String chat(List<ChatTurn> conversation) {
        List<ChatMessage> messages = new java.util.ArrayList<>();
        messages.add(new ChatMessage(
                "system",
                """
                You are a senior Korean economics specialist and tutor.
                Explain concepts as if speaking with a real economics expert.
                Stay focused on economics, finance, macroeconomics, microeconomics, investing, and policy.
                Prefer Korean in your answers.
                Keep explanations precise, structured, and practical.
                If the user's question is outside economics, answer briefly and steer back to economics context.
                """
        ));
        for (ChatTurn turn : conversation) {
            messages.add(new ChatMessage(turn.getRole(), turn.getContent()));
        }
        ChatRequest request = new ChatRequest(model, messages);
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
                You are an expert extractor of economics glossary terms.
                Task: Extract economics terms from the document for insertion into our database.
                Database schema (table: words):
                - id (auto-generated, ignore in output)
                - word (unique, required)
                - meaning (required)
                - english_word (nullable)
                - english_meaning (nullable)
                Only include terms whose meaning is explicitly stated in the document.
                Do NOT use external knowledge or guess missing meanings.
                If englishWord or englishMeaning are not explicitly stated, set them to null.
                Output format: valid JSON array ONLY. No markdown, no extra text.
                Each item must be an object with EXACT keys: word, meaning, englishWord, englishMeaning.
                Rules:
                - word must match the term as written in the document.
                - meaning must match the document statement; otherwise exclude the term.
                - englishWord and englishMeaning must come only from the document; otherwise null.
                - Remove duplicates case-insensitively and keep the best entry only.
                - Keep terms concise; exclude sentences and paragraphs.
                - If no valid terms exist, output [].
                Quality check internally before responding:
                1) Re-scan the document to avoid obvious misses.
                2) Verify every meaning is grounded in the document.
                3) Resolve duplicates to a single entry.
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
        String content = extractJsonPayload(response.choices.get(0).message.content);
        try {
            return objectMapper.readValue(content, new TypeReference<List<ExtractedTerm>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public DefinitionResult getDefinition(String term) {
        String prompt = """
                You are an expert extractor of economics glossary terms.
                Task: produce a database-ready definition for a single economics term.
                Database schema (table: words):
                - id (auto-generated, ignore in output)
                - word (unique, required)
                - meaning (required)
                - english_word (nullable)
                - english_meaning (nullable)
                Return ONLY valid JSON object with EXACT keys: word, meaning, englishWord, englishMeaning.
                Rules:
                - word must be the input term.
                - meaning must be a concise Korean explanation suitable for an economics glossary.
                - englishWord and englishMeaning should be null if uncertain.
                - No markdown, no prose, no code fences.
                Term: """ + term;
        ChatRequest request = new ChatRequest(model, List.of(
                new ChatMessage("system", "You answer in strict JSON only."),
                new ChatMessage("user", prompt)
        ));
        ChatResponse response = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block();
        if (response == null || response.choices == null || response.choices.isEmpty()) {
            return null;
        }
        String content = extractJsonPayload(response.choices.get(0).message.content);
        try {
            return objectMapper.readValue(content, DefinitionResult.class);
        } catch (Exception e) {
            DefinitionResult fallback = new DefinitionResult();
            fallback.setMeaning(content);
            fallback.setWord(term);
            return fallback;
        }
    }

    public DefinitionResult summarizeUploadedTerm(String term, String rawMeaning) {
        DefinitionResult fallback = new DefinitionResult();
        fallback.setWord(term);
        fallback.setMeaning(rawMeaning);

        if (term == null || term.isBlank() || rawMeaning == null || rawMeaning.isBlank()) {
            return fallback;
        }

        String prompt = """
                You are preparing a polished Korean economics glossary entry for end users.
                Return ONLY a valid JSON object with EXACT keys:
                word, meaning, englishWord, englishMeaning

                Output rules for meaning:
                1. The first line must be a short Korean explanation wrapped in double quotes.
                2. Add one blank line.
                3. Add one explanatory paragraph that starts with the term itself.
                4. Add one blank line.
                5. Add the heading **핵심 정리**
                6. Add 3 to 5 bullet points summarizing the concept.

                Output rules for englishWord and englishMeaning:
                - englishWord must be the commonly used English economics term.
                - englishMeaning must be a concise English explanation for internal storage.
                - If uncertain, use null.

                Style rules:
                - Prefer Korean for meaning.
                - Make the first line easy enough for a beginner to understand.
                - Keep the explanation polished and readable, similar to a well-formatted chat answer.
                - Use line breaks and emphasis naturally inside the meaning field.
                - Do not use code fences.
                - If the original meaning is brief, you may expand it into a helpful glossary explanation.
                - Stay economically accurate and centered on the term.

                Term: %s
                Original meaning: %s
                """.formatted(term, rawMeaning);

        ChatRequest request = new ChatRequest(model, List.of(
                new ChatMessage("system", "You return strict JSON for Korean economics glossary entries."),
                new ChatMessage("user", prompt)
        ));

        ChatResponse response = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block();

        if (response == null || response.choices == null || response.choices.isEmpty()) {
            return fallback;
        }

        String content = extractJsonPayload(response.choices.get(0).message.content);
        try {
            DefinitionResult parsed = objectMapper.readValue(content, DefinitionResult.class);
            if (parsed.getWord() == null || parsed.getWord().isBlank()) {
                parsed.setWord(term);
            }
            if (parsed.getMeaning() == null || parsed.getMeaning().isBlank()) {
                parsed.setMeaning(rawMeaning);
            }
            return parsed;
        } catch (Exception e) {
            return fallback;
        }
    }

    public DefinitionResult enrichImportedTerm(String term, String rawMeaning) {
        DefinitionResult fallback = new DefinitionResult();
        fallback.setWord(term);
        fallback.setMeaning(rawMeaning);

        if (term == null || term.isBlank()) {
            return fallback;
        }

        String prompt = """
                You are enriching an economics glossary entry for database storage.
                Return ONLY a valid JSON object with EXACT keys:
                word, meaning, englishWord, englishMeaning

                Rules:
                - word must equal the supplied term.
                - meaning must preserve the original Korean meaning with only minimal cleanup for spacing and punctuation.
                - englishWord must be the commonly used English economics term.
                - englishMeaning must be a concise English explanation for internal storage.
                - If uncertain, use null for englishWord or englishMeaning.
                - Do not use markdown, code fences, or extra prose.

                Term: %s
                Original meaning: %s
                """.formatted(term, rawMeaning == null ? "" : rawMeaning);

        ChatRequest request = new ChatRequest(model, List.of(
                new ChatMessage("system", "You return strict JSON for glossary enrichment."),
                new ChatMessage("user", prompt)
        ));

        ChatResponse response = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .block();

        if (response == null || response.choices == null || response.choices.isEmpty()) {
            return fallback;
        }

        String content = extractJsonPayload(response.choices.get(0).message.content);
        try {
            DefinitionResult parsed = objectMapper.readValue(content, DefinitionResult.class);
            if (parsed.getWord() == null || parsed.getWord().isBlank()) {
                parsed.setWord(term);
            }
            if (parsed.getMeaning() == null || parsed.getMeaning().isBlank()) {
                parsed.setMeaning(rawMeaning);
            }
            return parsed;
        } catch (Exception e) {
            return fallback;
        }
    }

    private String extractJsonPayload(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        if (arrayStart >= 0 && (objectStart < 0 || arrayStart < objectStart)) {
            int arrayEnd = trimmed.lastIndexOf(']');
            if (arrayEnd > arrayStart) {
                return trimmed.substring(arrayStart, arrayEnd + 1);
            }
        }
        if (objectStart >= 0) {
            int objectEnd = trimmed.lastIndexOf('}');
            if (objectEnd > objectStart) {
                return trimmed.substring(objectStart, objectEnd + 1);
            }
        }
        return trimmed;
    }

    public static class ExtractedTerm {
        private String word;
        private String meaning;
        private String englishWord;
        private String englishMeaning;
        private String source;

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

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    public static class DefinitionResult {
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

    public static class ChatTurn {
        private final String role;
        private final String content;

        public ChatTurn(String role, String content) {
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
