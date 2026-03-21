package com.economydict.batch;

import javax.sql.DataSource;
import java.sql.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
public class BatchSchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(BatchSchemaInitializer.class);

    @Bean
    public ApplicationRunner initializeBatchSchema(DataSource dataSource) {
        return args -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String databaseProductName = determineDatabaseProductName(dataSource);
            Integer tableCount = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM INFORMATION_SCHEMA.TABLES
                    WHERE UPPER(TABLE_NAME) = 'BATCH_JOB_INSTANCE'
                    """,
                    Integer.class
            );

            if (tableCount != null && tableCount > 0) {
                return;
            }

            String schemaResource = resolveSchemaResource(databaseProductName);
            if (schemaResource == null) {
                log.warn("Spring Batch metadata tables are missing, but no schema initializer is configured for database '{}'.", databaseProductName);
                return;
            }

            log.info("Spring Batch metadata tables are missing. Initializing {} batch schema.", databaseProductName);
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new ClassPathResource(schemaResource)
            );
            populator.execute(dataSource);
        };
    }

    private String determineDatabaseProductName(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (Exception ex) {
            log.warn("Failed to determine database product name for batch schema initialization.", ex);
            return "unknown";
        }
    }

    private String resolveSchemaResource(String databaseProductName) {
        if (databaseProductName == null) {
            return null;
        }
        String normalized = databaseProductName.toLowerCase();
        if (normalized.contains("postgres")) {
            return "org/springframework/batch/core/schema-postgresql.sql";
        }
        if (normalized.contains("h2")) {
            return "org/springframework/batch/core/schema-h2.sql";
        }
        return null;
    }
}
