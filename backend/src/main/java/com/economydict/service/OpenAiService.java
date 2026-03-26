package com.economydict.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class OpenAiService {
    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final String webSearchModel;
    private final ObjectMapper objectMapper;

    public OpenAiService(@Value("${openai.api.base-url}") String baseUrl,
                         @Value("${openai.api.key}") String apiKey,
                         @Value("${openai.api.model}") String model,
                         @Value("${openai.api.web-search-model:${openai.api.model}}") String webSearchModel) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.model = model;
        this.webSearchModel = webSearchModel;
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
        ChatResponse response = executeChatRequest(request);
        if (response == null || response.choices == null || response.choices.isEmpty()) {
            return "";
        }
        return response.choices.get(0).message.content;
    }

    public WebSearchResult respondWithWebSearch(String systemPrompt, String userPrompt) {
        return respondWithWebSearch(systemPrompt, List.of(new ChatTurn("user", userPrompt)));
    }

    public WebSearchResult respondWithWebSearch(String systemPrompt, List<ChatTurn> conversation) {
        JsonNode response = executeWebSearchRequest(buildWebSearchRequest(systemPrompt, conversation));
        String content = extractResponseMessageText(response);
        List<UrlCitation> citations = extractUrlCitations(response);
        List<WebSource> sources = extractSources(response);

        WebSearchResult result = new WebSearchResult();
        result.setContent(applyInlineCitations(content, citations));
        result.setSources(sources);
        return result;
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
        ChatResponse response = executeChatRequest(request);
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
        ChatResponse response = executeChatRequest(request);
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

        ChatResponse response = executeChatRequest(request);

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

        ChatResponse response = executeChatRequest(request);

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

    public DefinitionResult translateTermToEnglish(String term, String rawMeaning) {
        DefinitionResult fallback = new DefinitionResult();
        fallback.setWord(term);
        fallback.setMeaning(rawMeaning);

        if (term == null || term.isBlank()) {
            return fallback;
        }

        String prompt = """
                You are translating Korean economics glossary entries for database storage.
                Return ONLY a valid JSON object with EXACT keys:
                word, meaning, englishWord, englishMeaning

                Rules:
                - word must equal the supplied Korean term.
                - meaning must stay unchanged from the supplied Korean meaning.
                - englishWord must be the most common English economics term.
                - englishMeaning must be a concise English explanation suitable for storage.
                - If uncertain, use null for englishWord or englishMeaning.
                - Do not use markdown, code fences, or extra prose.

                Term: %s
                Korean meaning: %s
                """.formatted(term, rawMeaning == null ? "" : rawMeaning);

        ChatRequest request = new ChatRequest(model, List.of(
                new ChatMessage("system", "You return strict JSON for glossary translation."),
                new ChatMessage("user", prompt)
        ));

        ChatResponse response = executeChatRequest(request);

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

    public List<GeneratedQuizItem> generateQuizItems(List<QuizGenerationSeed> seeds) {
        if (seeds == null || seeds.isEmpty()) {
            return Collections.emptyList();
        }

        List<GeneratedQuizItem> items = new ArrayList<>();
        for (QuizGenerationSeed seed : seeds) {
            items.add(generateQuizItem(seed));
        }
        return items;
    }

    private GeneratedQuizItem generateQuizItem(QuizGenerationSeed seed) {
        try {
            String seedPayload = objectMapper.writeValueAsString(seed);
            String prompt = """
                    You are generating one Korean multiple-choice economics quiz item for database insertion.
                    Return ONLY a valid JSON object.

                    Output schema:
                    {
                      "word": "정답 용어",
                      "questionText": "정답 용어를 직접 쓰지 않는 한국어 질문문",
                      "options": ["보기1", "보기2", "보기3", "보기4"]
                    }

                    Rules:
                    - word must exactly match the input word.
                    - questionText must be in Korean, concise, and must not contain the answer word itself.
                    - options must contain exactly 4 unique strings.
                    - options must include the correct word exactly once.
                    - The remaining 3 options must be selected only from distractorCandidates.word.
                    - Do not invent new terms outside the allowed option pool.
                    - Keep all options as economics terms, not explanations.
                    - No markdown, no prose, no code fences.
                    - Before responding, internally verify that the object has 4 unique options and the correct word appears exactly once.

                    Compact example:
                    {"word":"유동성 함정","questionText":"금리가 매우 낮아도 통화정책 효과가 약해지는 상황을 가리키는 용어는 무엇인가?","options":["유동성 함정","구축 효과","기저 효과","승수 효과"]}

                    Input:
                    """ + seedPayload;

            ChatRequest request = new ChatRequest(model, List.of(
                    new ChatMessage("system", "You return strict JSON for economics quiz generation."),
                    new ChatMessage("user", prompt)
            ));

            ChatResponse response = executeChatRequest(request);

            if (response == null || response.choices == null || response.choices.isEmpty()) {
                throw new IllegalStateException("OpenAI가 퀴즈 문항 응답을 비워서 반환했습니다: " + seed.getWord());
            }

            String content = extractJsonPayload(response.choices.get(0).message.content);
            if (content.trim().startsWith("[")) {
                List<GeneratedQuizItem> parsed = objectMapper.readValue(content, new TypeReference<List<GeneratedQuizItem>>() {});
                if (parsed.isEmpty()) {
                    throw new IllegalStateException("OpenAI가 퀴즈 문항 배열을 비워서 반환했습니다: " + seed.getWord());
                }
                return parsed.get(0);
            }
            return objectMapper.readValue(content, GeneratedQuizItem.class);
        } catch (WebClientResponseException e) {
            throw new IllegalStateException("OpenAI 요청이 실패했습니다. status=" + e.getStatusCode().value() + " body=" + summarizeErrorBody(e.getResponseBodyAsString()), e);
        } catch (WebClientRequestException e) {
            throw new IllegalStateException("OpenAI 서버에 연결하지 못했습니다: " + e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI 퀴즈 응답을 해석하지 못했습니다: " + seed.getWord(), e);
        }
    }

    private String summarizeErrorBody(String body) {
        if (body == null) {
            return "";
        }
        String normalized = body.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 240) {
            return normalized;
        }
        return normalized.substring(0, 240);
    }

    private ChatResponse executeChatRequest(ChatRequest request) {
        if (apiKey == null || apiKey.isBlank() || "REPLACE_ME".equals(apiKey)) {
            throw new IllegalStateException(
                    "OpenAI API key is not configured. Set OPENAI_API_KEY or provide openai.api.key in secrets.json.");
        }
        try {
            return webClient.post()
                    .uri("/v1/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "OpenAI request failed. status=" + e.getStatusCode().value()
                            + " body=" + summarizeErrorBody(e.getResponseBodyAsString()),
                    e);
        } catch (WebClientRequestException e) {
            throw new IllegalStateException("OpenAI server connection failed. " + e.getMessage(), e);
        }
    }

    private JsonNode executeWebSearchRequest(Map<String, Object> request) {
        if (apiKey == null || apiKey.isBlank() || "REPLACE_ME".equals(apiKey)) {
            throw new IllegalStateException(
                    "OpenAI API key is not configured. Set OPENAI_API_KEY or provide openai.api.key in secrets.json.");
        }
        try {
            return webClient.post()
                    .uri("/v1/responses")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "OpenAI web search request failed. status=" + e.getStatusCode().value()
                            + " body=" + summarizeErrorBody(e.getResponseBodyAsString()),
                    e);
        } catch (WebClientRequestException e) {
            throw new IllegalStateException("OpenAI web search connection failed. " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildWebSearchRequest(String systemPrompt, List<ChatTurn> conversation) {
        List<Map<String, Object>> input = new ArrayList<>();
        input.add(buildInputMessage("system", systemPrompt));

        if (conversation != null) {
            for (ChatTurn turn : conversation) {
                if (turn == null || turn.getContent() == null || turn.getContent().isBlank()) {
                    continue;
                }
                input.add(buildInputMessage(turn.getRole(), turn.getContent()));
            }
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", webSearchModel == null || webSearchModel.isBlank() ? model : webSearchModel);
        request.put("reasoning", Map.of("effort", "medium"));
        request.put("tools", List.of(Map.of("type", "web_search")));
        request.put("tool_choice", "auto");
        request.put("include", List.of("web_search_call.action.sources"));
        request.put("input", input);
        return request;
    }

    private Map<String, Object> buildInputMessage(String role, String text) {
        String resolvedRole = role == null || role.isBlank() ? "user" : role;
        String contentType = "assistant".equals(resolvedRole) ? "output_text" : "input_text";
        return Map.of(
                "role", resolvedRole,
                "content", List.of(Map.of(
                        "type", contentType,
                        "text", text
                ))
        );
    }

    private String extractResponseMessageText(JsonNode response) {
        if (response == null) {
            return "";
        }

        JsonNode output = response.path("output");
        if (!output.isArray()) {
            return response.path("output_text").asText("");
        }

        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText())) {
                continue;
            }
            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if ("output_text".equals(contentItem.path("type").asText())) {
                    return contentItem.path("text").asText("");
                }
            }
        }

        return response.path("output_text").asText("");
    }

    private List<UrlCitation> extractUrlCitations(JsonNode response) {
        if (response == null) {
            return List.of();
        }

        List<UrlCitation> citations = new ArrayList<>();
        JsonNode output = response.path("output");
        if (!output.isArray()) {
            return citations;
        }

        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asText())) {
                continue;
            }

            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }

            for (JsonNode contentItem : content) {
                JsonNode annotations = contentItem.path("annotations");
                if (!annotations.isArray()) {
                    continue;
                }
                for (JsonNode annotation : annotations) {
                    JsonNode citationNode = resolveCitationNode(annotation);
                    String url = citationNode.path("url").asText("");
                    if (url.isBlank()) {
                        continue;
                    }

                    UrlCitation citation = new UrlCitation();
                    citation.setUrl(url);
                    citation.setTitle(citationNode.path("title").asText(""));
                    citation.setStartIndex(citationNode.path("start_index").asInt(-1));
                    citation.setEndIndex(citationNode.path("end_index").asInt(-1));
                    citations.add(citation);
                }
            }
        }

        return citations;
    }

    private JsonNode resolveCitationNode(JsonNode annotation) {
        if (annotation == null) {
            return objectMapper.createObjectNode();
        }
        if ("url_citation".equals(annotation.path("type").asText()) && annotation.hasNonNull("url_citation")) {
            return annotation.path("url_citation");
        }
        return annotation;
    }

    private List<WebSource> extractSources(JsonNode response) {
        if (response == null) {
            return List.of();
        }

        Map<String, WebSource> deduped = new LinkedHashMap<>();
        JsonNode output = response.path("output");
        if (!output.isArray()) {
            return List.of();
        }

        for (JsonNode item : output) {
            if (!"web_search_call".equals(item.path("type").asText())) {
                continue;
            }

            JsonNode sources = item.path("action").path("sources");
            if (!sources.isArray()) {
                continue;
            }

            for (JsonNode sourceNode : sources) {
                String url = sourceNode.path("url").asText("");
                if (url.isBlank() || deduped.containsKey(url)) {
                    continue;
                }

                WebSource source = new WebSource();
                source.setUrl(url);
                source.setTitle(sourceNode.path("title").asText(deriveDomain(url)));
                source.setDomain(deriveDomain(url));
                deduped.put(url, source);
            }
        }

        return new ArrayList<>(deduped.values());
    }

    private String applyInlineCitations(String content, List<UrlCitation> citations) {
        if (content == null || content.isBlank() || citations == null || citations.isEmpty()) {
            return content == null ? "" : content;
        }

        List<UrlCitation> validCitations = citations.stream()
                .filter(citation -> citation.getUrl() != null && !citation.getUrl().isBlank())
                .filter(citation -> citation.getEndIndex() >= 0)
                .sorted(Comparator.comparingInt(UrlCitation::getEndIndex).reversed())
                .toList();

        if (validCitations.isEmpty()) {
            return content;
        }

        Map<String, Integer> sourceNumbers = new LinkedHashMap<>();
        for (UrlCitation citation : citations) {
            if (citation.getUrl() == null || citation.getUrl().isBlank()) {
                continue;
            }
            sourceNumbers.computeIfAbsent(citation.getUrl(), ignored -> sourceNumbers.size() + 1);
        }

        StringBuilder builder = new StringBuilder(content);
        for (UrlCitation citation : validCitations) {
            int endIndex = Math.max(0, Math.min(citation.getEndIndex(), builder.length()));
            Integer sourceNumber = sourceNumbers.get(citation.getUrl());
            if (sourceNumber == null) {
                continue;
            }
            builder.insert(endIndex, " [" + sourceNumber + "](" + citation.getUrl() + ")");
        }
        return builder.toString();
    }

    private String deriveDomain(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? url : uri.getHost();
        } catch (IllegalArgumentException e) {
            return url;
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

    public static class QuizGenerationSeed {
        private String word;
        private String meaning;
        private List<QuizCandidate> distractorCandidates = Collections.emptyList();

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

        public List<QuizCandidate> getDistractorCandidates() {
            return distractorCandidates;
        }

        public void setDistractorCandidates(List<QuizCandidate> distractorCandidates) {
            this.distractorCandidates = distractorCandidates;
        }
    }

    public static class QuizCandidate {
        private String word;
        private String meaning;

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
    }

    public static class GeneratedQuizItem {
        private String word;
        private String questionText;
        private List<String> options = Collections.emptyList();

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }
    }

    public static class WebSearchResult {
        private String content;
        private List<WebSource> sources = Collections.emptyList();

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public List<WebSource> getSources() {
            return sources;
        }

        public void setSources(List<WebSource> sources) {
            this.sources = sources == null ? Collections.emptyList() : sources;
        }
    }

    public static class WebSource {
        private String title;
        private String url;
        private String domain;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }
    }

    private static class UrlCitation {
        private String title;
        private String url;
        private int startIndex;
        private int endIndex;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public void setEndIndex(int endIndex) {
            this.endIndex = endIndex;
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
