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
                log.info("Ensured words.meaning and words.english_meaning use TEXT in PostgreSQL.");
                return;
            }

            if (normalized.contains("h2")) {
                jdbcTemplate.execute("ALTER TABLE words ALTER COLUMN meaning CLOB");
                jdbcTemplate.execute("ALTER TABLE words ALTER COLUMN english_meaning CLOB");
                return;
            }

            log.warn("Dictionary schema initializer skipped for unsupported database '{}'.", databaseProductName);
        };
    }

    private boolean wordsTableExists(JdbcTemplate jdbcTemplate) {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE UPPER(TABLE_NAME) = 'WORDS'
                """,
                Integer.class
        );
        return tableCount != null && tableCount > 0;
    }

    private String determineDatabaseProductName(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (Exception ex) {
            log.warn("Failed to determine database product name for dictionary schema initialization.", ex);
            return null;
        }
    }
}
