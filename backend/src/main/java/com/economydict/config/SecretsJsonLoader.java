package com.economydict.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SecretsJsonLoader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<Path> CANDIDATE_PATHS = List.of(
            Path.of("secrets.json"),
            Path.of("../secrets.json"),
            Path.of("/app/secrets.json")
    );

    private SecretsJsonLoader() {
    }

    public static Map<String, Object> load() {
        for (Path candidate : CANDIDATE_PATHS) {
            Path resolved = candidate.toAbsolutePath().normalize();
            if (!Files.isRegularFile(resolved)) {
                continue;
            }
            try {
                Map<String, Object> raw = OBJECT_MAPPER.readValue(resolved.toFile(), new TypeReference<>() {});
                Map<String, Object> flattened = new LinkedHashMap<>();
                flatten("secrets", raw, flattened);
                return flattened;
            } catch (IOException ignored) {
                return Map.of();
            }
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> source, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flatten(key, (Map<String, Object>) nested, target);
                continue;
            }
            target.put(key, value);
        }
    }
}
