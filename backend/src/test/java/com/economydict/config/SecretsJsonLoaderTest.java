package com.economydict.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecretsJsonLoaderTest {

    @Test
    void loadsSecretsUsingSpringPropertyNames(@TempDir Path tempDir) throws IOException {
        Path secretsFile = tempDir.resolve("secrets.json");
        Files.writeString(secretsFile, """
                {
                  "app": {
                    "jwt": {
                      "secret": "jwt-secret"
                    }
                  },
                  "openai": {
                    "api": {
                      "key": "test-openai-key",
                      "model": "gpt-4o-mini"
                    }
                  }
                }
                """, StandardCharsets.UTF_8);

        Map<String, Object> loaded = SecretsJsonLoader.load(List.of(secretsFile));

        assertThat(loaded)
                .containsEntry("secrets.app.jwt.secret", "jwt-secret")
                .containsEntry("secrets.openai.api.key", "test-openai-key")
                .containsEntry("secrets.openai.api.model", "gpt-4o-mini");
    }

    @Test
    void skipsInvalidCandidateAndUsesNextAvailableFile(@TempDir Path tempDir) throws IOException {
        Path invalidFile = tempDir.resolve("invalid-secrets.json");
        Path validFile = tempDir.resolve("valid-secrets.json");
        Files.writeString(invalidFile, "{ invalid json", StandardCharsets.UTF_8);
        Files.writeString(validFile, """
                {
                  "openai": {
                    "api": {
                      "key": "fallback-key"
                    }
                  }
                }
                """, StandardCharsets.UTF_8);

        Map<String, Object> loaded = SecretsJsonLoader.load(List.of(invalidFile, validFile));

        assertThat(loaded).containsEntry("secrets.openai.api.key", "fallback-key");
    }

    @Test
    void appliesSecretsAsSystemPropertiesWhenEnvUsesPlaceholder() {
        String key = "openai.api.key";
        String previous = System.getProperty(key);
        System.setProperty(key, "REPLACE_ME");
        try {
            SecretsJsonLoader.applySystemPropertyOverrides(Map.of("secrets.openai.api.key", "secret-key"));

            assertThat(System.getProperty(key)).isEqualTo("secret-key");
        } finally {
            restoreSystemProperty(key, previous);
        }
    }

    @Test
    void keepsExplicitSystemPropertyValue() {
        String key = "openai.api.key";
        String previous = System.getProperty(key);
        System.setProperty(key, "explicit-key");
        try {
            SecretsJsonLoader.applySystemPropertyOverrides(Map.of("secrets.openai.api.key", "secret-key"));

            assertThat(System.getProperty(key)).isEqualTo("explicit-key");
        } finally {
            restoreSystemProperty(key, previous);
        }
    }

    private void restoreSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, value);
    }
}
