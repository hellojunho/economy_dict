package com.economydict.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SecretsJsonLoader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SECRETS_PREFIX = "secrets.";

    private SecretsJsonLoader() {
    }

    public static Map<String, Object> load() {
        return load(candidatePaths());
    }

    static Map<String, Object> load(List<Path> candidatePaths) {
        for (Path candidate : candidatePaths) {
            Path resolved = candidate.toAbsolutePath().normalize();
            if (!Files.isRegularFile(resolved)) {
                continue;
            }
            try {
                Map<String, Object> raw = OBJECT_MAPPER.readValue(resolved.toFile(), new TypeReference<>() {});
                Map<String, Object> flattened = new LinkedHashMap<>();
                flatten(SECRETS_PREFIX.substring(0, SECRETS_PREFIX.length() - 1), raw, flattened);
                return flattened;
            } catch (IOException ignored) {
                continue;
            }
        }
        return Map.of();
    }

    public static void applySystemPropertyOverrides(Map<String, Object> secrets) {
        for (Map.Entry<String, Object> entry : secrets.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(SECRETS_PREFIX)) {
                continue;
            }
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            String propertyKey = key.substring(SECRETS_PREFIX.length());
            if (propertyKey.isBlank()) {
                continue;
            }

            String configuredSystemValue = System.getProperty(propertyKey);
            if (!isUnsetOrPlaceholder(configuredSystemValue)) {
                continue;
            }

            String envValue = System.getenv(toEnvKey(propertyKey));
            if (!isUnsetOrPlaceholder(envValue)) {
                continue;
            }

            System.setProperty(propertyKey, String.valueOf(value));
        }
    }

    private static List<Path> candidatePaths() {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of("secrets.json"));
        candidates.add(Path.of("../secrets.json"));
        candidates.add(Path.of("/app/secrets.json"));

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            Path cwd = Path.of(userDir).toAbsolutePath().normalize();
            candidates.add(cwd.resolve("secrets.json"));
            candidates.add(cwd.resolve("../secrets.json"));
            candidates.add(cwd.resolve("../../secrets.json"));
        }

        return candidates.stream().distinct().toList();
    }

    private static String toEnvKey(String propertyKey) {
        return propertyKey.replace('.', '_').replace('-', '_').toUpperCase();
    }

    private static boolean isUnsetOrPlaceholder(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return "REPLACE_ME".equals(value) || value.startsWith("CHANGE_ME");
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> source, Map<String, Object> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix == null || prefix.isBlank()
                    ? entry.getKey()
                    : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flatten(key, (Map<String, Object>) nested, target);
                continue;
            }
            target.put(key, value);
        }
    }
}
