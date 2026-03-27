package com.economydict.service;

import com.economydict.entity.DictionaryEntry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DictionaryMeaningFormatService {
    private static final Pattern BULLET_PREFIX = Pattern.compile("^[-*•]\\s+");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");
    private static final Pattern SUMMARY_HEADING_PREFIX = Pattern.compile("^(핵심\\s*정리)\\s*[-:：]?\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUMMARY_HEADING_ANYWHERE = Pattern.compile("핵심\\s*정리\\s*[-:：]?", Pattern.CASE_INSENSITIVE);
    private static final int MAX_BULLET_LENGTH = 140;

    public String formatMeaning(String word, String rawMeaning) {
        String normalizedWord = normalize(word);
        String normalizedMeaning = normalizeMultiline(rawMeaning);
        if (normalizedWord == null || normalizedMeaning == null) {
            return rawMeaning;
        }

        ParsedMeaning parsed = parseMeaning(normalizedMeaning);
        List<String> sentences = parsed.sentences();
        List<String> bullets = parsed.bullets();

        String summarySource = firstNonBlank(sentences.isEmpty() ? null : sentences.get(0), normalizedMeaning);
        String summary = ensureSentence(wordify(normalizedWord, summarySource));

        String paragraph = buildParagraph(normalizedWord, sentences, flatten(normalizedMeaning));
        List<String> bulletLines = buildBullets(normalizedWord, bullets, sentences, paragraph);

        return String.join(
                "\n\n",
                "**\"" + stripOuterQuotes(summary) + "\"**",
                paragraph,
                "핵심 정리\n\n" + String.join("\n", bulletLines)
        ).trim();
    }

    public boolean applyFormat(DictionaryEntry entry) {
        if (entry == null) {
            return false;
        }

        String formattedMeaning = formatMeaning(entry.getWord(), entry.getMeaning());
        boolean changed = formattedMeaning != null && !formattedMeaning.equals(entry.getMeaning());
        if (changed) {
            entry.setMeaning(formattedMeaning);
        }
        return changed;
    }

    private ParsedMeaning parseMeaning(String rawMeaning) {
        Set<String> bulletSet = new LinkedHashSet<>();
        List<String> proseLines = new ArrayList<>();
        SummarySections sections = splitSummarySections(rawMeaning);

        for (String rawLine : sections.body().replace("\r\n", "\n").split("\n")) {
            String line = stripFormatting(rawLine);
            if (line.isBlank()) {
                continue;
            }
            if (isSummaryHeading(line)) {
                continue;
            }
            if (BULLET_PREFIX.matcher(line).find()) {
                addUnique(bulletSet, cleanBullet(line));
                continue;
            }
            String cleanedLine = cleanSummaryHeadingPrefix(line);
            if (cleanedLine != null) {
                proseLines.add(cleanedLine);
            }
        }

        for (String bullet : extractBulletCandidates(sections.summary())) {
            addUnique(bulletSet, bullet);
        }

        String prose = String.join(" ", proseLines).replaceAll("\\s+", " ").trim();
        Set<String> sentenceSet = new LinkedHashSet<>();
        if (!prose.isBlank()) {
            for (String part : SENTENCE_SPLIT.split(prose)) {
                String sentence = normalizeSentence(part);
                if (sentence != null) {
                    addUnique(sentenceSet, sentence);
                }
            }
        }

        List<String> sentences = new ArrayList<>(sentenceSet);
        if (sentences.isEmpty() && !prose.isBlank()) {
            sentences.add(ensureSentence(prose));
        }

        return new ParsedMeaning(sentences, new ArrayList<>(bulletSet));
    }

    private String buildParagraph(String word, List<String> sentences, String fallbackMeaning) {
        List<String> paragraphSentences = new ArrayList<>();
        for (String sentence : sentences) {
            if (paragraphSentences.size() >= 2) {
                break;
            }
            addUnique(paragraphSentences, harmonizeLeadingTopic(sentence, word));
        }

        if (paragraphSentences.size() == 1) {
            paragraphSentences.add("이 개념은 관련 경제 현상을 이해하는 기본 출발점이 됩니다.");
        }

        String paragraph = paragraphSentences.isEmpty() ? fallbackMeaning : String.join(" ", paragraphSentences);
        paragraph = paragraph.replaceAll("\\s+", " ").trim();
        if (!paragraph.startsWith(word)) {
            paragraph = wordify(word, paragraph);
        } else {
            paragraph = harmonizeLeadingTopic(paragraph, word);
        }
        return ensureSentence(paragraph);
    }

    private List<String> buildBullets(String word, List<String> existingBullets, List<String> sentences, String paragraph) {
        Set<String> items = new LinkedHashSet<>();
        String coreSentence = SENTENCE_SPLIT.split(paragraph)[0];
        for (String candidate : splitSentenceIntoBullets(coreSentence, word)) {
            if (isUsefulBullet(candidate)) {
                addUnique(items, candidate);
            }
            if (items.size() >= 3) {
                break;
            }
        }

        if (items.size() < 3) {
            items.add("개념의 핵심 구조와 역할을 빠르게 파악할 수 있습니다.");
            items.add("경제 지표와 산업 흐름을 해석할 때 함께 활용됩니다.");
            items.add("경제 기사나 보고서를 읽을 때 기본 개념으로 자주 등장합니다.");
        }

        List<String> result = new ArrayList<>();
        for (String item : items) {
            if (item == null || item.isBlank()) {
                continue;
            }
            result.add("- " + item);
            if (result.size() >= 5) {
                break;
            }
        }
        return result;
    }

    private String cleanBullet(String value) {
        return stripFormatting(BULLET_PREFIX.matcher(value == null ? "" : value.trim()).replaceFirst(""));
    }

    private boolean isSummaryHeading(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return false;
        }
        return "핵심 정리".equals(normalized) || "핵심정리".equals(normalized);
    }

    private String cleanSummaryHeadingPrefix(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        String stripped = SUMMARY_HEADING_PREFIX.matcher(normalized).replaceFirst("").trim();
        return stripped.isBlank() ? null : stripped;
    }

    private SummarySections splitSummarySections(String rawMeaning) {
        String normalized = rawMeaning == null ? "" : rawMeaning.replace("\r\n", "\n");
        var matcher = SUMMARY_HEADING_ANYWHERE.matcher(normalized);
        if (!matcher.find()) {
            return new SummarySections(normalized, "");
        }
        String body = normalized.substring(0, matcher.start()).trim();
        String summary = normalized.substring(matcher.end()).trim();
        return new SummarySections(body, summary);
    }

    private List<String> extractBulletCandidates(String rawSummarySection) {
        List<String> bullets = new ArrayList<>();
        if (rawSummarySection == null || rawSummarySection.isBlank()) {
            return bullets;
        }

        String normalized = rawSummarySection
                .replace("\r\n", "\n")
                .replaceAll("\n+", "\n");
        normalized = SUMMARY_HEADING_ANYWHERE.matcher(normalized).replaceAll("\n");
        normalized = normalized.replaceAll("(?m)^\\s*[-*•]\\s*", "\n");
        normalized = normalized.replaceAll("\\s+-\\s+", "\n");

        for (String rawPart : normalized.split("\n")) {
            String part = normalizeBulletCandidate(cleanBullet(stripFormatting(rawPart)));
            if (part == null || part.isBlank()) {
                continue;
            }
            bullets.add(part);
        }
        return bullets;
    }

    private String stripFormatting(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("**", "")
                .replace("##", "")
                .replace("\"", "")
                .trim();
    }

    private String normalizeSentence(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return ensureSentence(cleanSummaryHeadingPrefix(normalized));
    }

    private String ensureSentence(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        char last = normalized.charAt(normalized.length() - 1);
        if (last == '.' || last == '!' || last == '?') {
            return normalized;
        }
        return normalized + ".";
    }

    private String stripWordPrefix(String sentence, String word) {
        if (sentence == null || word == null) {
            return sentence;
        }
        String normalized = sentence.trim();
        String[] prefixes = {word + "는 ", word + "은 "};
        for (String prefix : prefixes) {
            if (normalized.startsWith(prefix)) {
                return normalized.substring(prefix.length()).trim();
            }
        }
        return normalized;
    }

    private String wordify(String word, String sentence) {
        String normalized = normalize(sentence);
        if (word == null || normalized == null) {
            return sentence;
        }
        if (normalized.startsWith(word)) {
            return harmonizeLeadingTopic(normalized, word);
        }
        return word + topicMarker(word) + " " + stripOuterQuotes(normalized);
    }

    private String harmonizeLeadingTopic(String sentence, String word) {
        String normalized = normalize(sentence);
        if (normalized == null || word == null || word.isBlank() || !normalized.startsWith(word)) {
            return normalized;
        }

        String remainder = normalized.substring(word.length());
        if (remainder.startsWith("는 ") || remainder.startsWith("은 ")) {
            return word + topicMarker(word) + remainder.substring(1);
        }
        if (remainder.startsWith("는") || remainder.startsWith("은")) {
            return word + topicMarker(word) + remainder.substring(1);
        }
        return normalized;
    }

    private List<String> splitSentenceIntoBullets(String sentence, String word) {
        List<String> result = new ArrayList<>();
        String base = cleanSummaryHeadingPrefix(stripWordPrefix(sentence, word));
        if (base == null) {
            return result;
        }

        for (String clause : base.split("\\s*,\\s*")) {
            String normalizedClause = normalize(clause);
            if (normalizedClause == null) {
                continue;
            }
            result.add(ensureSentence(normalizedClause));
        }

        if (result.isEmpty()) {
            result.add(ensureSentence(base));
        }
        return result;
    }

    private boolean isUsefulBullet(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return false;
        }
        if (normalized.contains("핵심 정리") || normalized.contains("핵심정리")) {
            return false;
        }
        if (normalized.length() > MAX_BULLET_LENGTH) {
            return false;
        }
        if (normalized.contains("- -") || normalized.contains("--")) {
            return false;
        }
        return !normalized.replaceAll("[^0-9A-Za-z가-힣]", "").trim().isBlank();
    }

    private String normalizeBulletCandidate(String value) {
        String cleaned = cleanSummaryHeadingPrefix(cleanBullet(value));
        if (cleaned == null) {
            return null;
        }

        String normalized = cleaned
                .replaceAll("\\s*(핵심\\s*정리\\s*[-:：]?)\\s*", " ")
                .replaceAll("\\s+-\\s+", " ")
                .replaceAll("-{2,}", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isBlank()) {
            return null;
        }
        return ensureSentence(normalized);
    }

    private String stripOuterQuotes(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return "";
        }
        String stripped = normalized;
        while (stripped.startsWith("\"") || stripped.endsWith("\"")) {
            stripped = stripped.replaceAll("^\"+|\"+$", "").trim();
        }
        return stripped;
    }

    private String firstNonBlank(String primary, String fallback) {
        String first = normalize(primary);
        return first != null ? first : fallback;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace("\r\n", "\n").replace('\u00A0', ' ').trim();
        normalized = normalized.replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeMultiline(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.replace("\r\n", "\n").replace('\u00A0', ' ').trim();
        if (normalized.isBlank()) {
            return null;
        }

        String[] rawLines = normalized.split("\n", -1);
        List<String> lines = new ArrayList<>();
        boolean previousBlank = false;
        for (String rawLine : rawLines) {
            String line = rawLine.replaceAll("[ \\t]+", " ").trim();
            if (line.isBlank()) {
                if (!previousBlank) {
                    lines.add("");
                }
                previousBlank = true;
                continue;
            }
            lines.add(line);
            previousBlank = false;
        }
        return String.join("\n", lines).trim();
    }

    private String flatten(String value) {
        if (value == null) {
            return null;
        }
        return normalize(value.replace('\n', ' '));
    }

    private void addUnique(Set<String> target, String candidate) {
        String normalized = normalize(candidate);
        if (normalized == null) {
            return;
        }
        String key = comparableKey(normalized);
        boolean exists = target.stream().anyMatch(item -> comparableKey(item).equals(key));
        if (!exists) {
            target.add(normalized);
        }
    }

    private void addUnique(List<String> target, String candidate) {
        String normalized = normalize(candidate);
        if (normalized == null) {
            return;
        }
        String key = comparableKey(normalized);
        boolean exists = target.stream().anyMatch(item -> comparableKey(item).equals(key));
        if (!exists) {
            target.add(normalized);
        }
    }

    private String comparableKey(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return "";
        }
        String cleaned = cleanSummaryHeadingPrefix(stripOuterQuotes(normalized));
        if (cleaned == null) {
            return "";
        }
        return cleaned
                .replaceAll("^[-*•]\\s*", "")
                .replaceAll("[\"'.,!?]", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    private String topicMarker(String word) {
        if (word == null || word.isBlank()) {
            return "는";
        }
        char last = word.charAt(word.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) {
            return "는";
        }
        int jongseong = (last - 0xAC00) % 28;
        return jongseong == 0 ? "는" : "은";
    }

    private record ParsedMeaning(List<String> sentences, List<String> bullets) {
    }

    private record SummarySections(String body, String summary) {
    }
}
