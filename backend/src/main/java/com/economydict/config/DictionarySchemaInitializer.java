package com.economydict.config;

import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DictionarySchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(DictionarySchemaInitializer.class);

    @Bean
    public ApplicationRunner initializeDictionarySchema(DataSource dataSource) {
        return args -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            if (!wordsTableExists(jdbcTemplate)) {
                return;
            }

            String databaseProductName = determineDatabaseProductName(dataSource);
            if (databaseProductName == null) {
                return;
            }

            String normalized = databaseProductName.toLowerCase();
            if (normalized.contains("postgres")) {
                jdbcTemplate.execute("ALTER TABLE words ALTER COLUMN meaning TYPE TEXT");
                jdbcTemplate.execute("ALTER TABLE words ALTER COLUMN english_meaning TYPE TEXT");
                seedFileTypes(jdbcTemplate);
                seedSources(jdbcTemplate);
                migrateLegacySourceColumn(jdbcTemplate);
                log.info("Ensured words.meaning and words.english_meaning use TEXT in PostgreSQL.");
                return;
            }

            if (normalized.contains("h2")) {
                jdbcTemplate.execute("ALTER TABLE words ALTER COLUMN meaning CLOB");
                jdbcTemplate.execute("ALTER TABLE words ALTER COLUMN english_meaning CLOB");
                seedFileTypes(jdbcTemplate);
                seedSources(jdbcTemplate);
                migrateLegacySourceColumn(jdbcTemplate);
                return;
            }

            log.warn("Dictionary schema initializer skipped for unsupported database '{}'.", databaseProductName);
        };
    }

    private boolean wordsTableExists(JdbcTemplate jdbcTemplate) {
        return tableExists(jdbcTemplate, "words");
    }

    private String determineDatabaseProductName(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (Exception ex) {
            log.warn("Failed to determine database product name for dictionary schema initialization.", ex);
            return null;
        }
    }

    private void seedFileTypes(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate, "file_type")) {
            return;
        }
        upsertFileType(jdbcTemplate, "MANUAL");
        upsertFileType(jdbcTemplate, "AI_IMPORT");
        upsertFileType(jdbcTemplate, "JSON_IMPORT");
        upsertFileType(jdbcTemplate, "AI_LOOKUP");
    }

    private void seedSources(JdbcTemplate jdbcTemplate) {
        if (!tableExists(jdbcTemplate, "source")) {
            return;
        }
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM source WHERE LOWER(name) = LOWER(?)",
                Integer.class,
                "한국경제용어 700선"
        );
        if (existing == null || existing == 0) {
            jdbcTemplate.update("INSERT INTO source (name) VALUES (?)", "한국경제용어 700선");
        }
    }

    private void migrateLegacySourceColumn(JdbcTemplate jdbcTemplate) {
        if (!columnExists(jdbcTemplate, "words", "file_type_code")) {
            return;
        }

        if (columnExists(jdbcTemplate, "words", "source")) {
            jdbcTemplate.update("""
                    UPDATE words
                    SET file_type_code = UPPER(REPLACE(REPLACE(TRIM(source), '-', '_'), ' ', '_'))
                    WHERE file_type_code IS NULL
                      AND source IS NOT NULL
                      AND TRIM(source) <> ''
                    """);
        }

        jdbcTemplate.update("UPDATE words SET file_type_code = 'MANUAL' WHERE file_type_code IS NULL OR TRIM(file_type_code) = ''");

        if (columnExists(jdbcTemplate, "words", "source")) {
            jdbcTemplate.execute("ALTER TABLE words DROP COLUMN source");
        }
    }

    private void upsertFileType(JdbcTemplate jdbcTemplate, String code) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM file_type WHERE code = ?",
                Integer.class,
                code
        );
        if (existing == null || existing == 0) {
            jdbcTemplate.update("INSERT INTO file_type (code, display_name) VALUES (?, ?)", code, code);
        }
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE UPPER(TABLE_NAME) = UPPER(?)
                """,
                Integer.class,
                tableName
        );
        return tableCount != null && tableCount > 0;
    }

    private boolean columnExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE UPPER(TABLE_NAME) = UPPER(?)
                  AND UPPER(COLUMN_NAME) = UPPER(?)
                """,
                Integer.class,
                tableName,
                columnName
        );
        return columnCount != null && columnCount > 0;
    }
}
