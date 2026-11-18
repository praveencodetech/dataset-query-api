package com.assignment.dataset.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Production DataSource configuration active only in the 'prod' profile.
 * Reads DATABASE_URL (handling Render's postgres:// format via DatabaseUrlConverter)
 * and configures a resilient PostgreSQL Hikari connection pool.
 */
@Configuration
@Profile("prod")
public class ProdDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(ProdDataSourceConfig.class);

    private final DatabaseUrlConverter urlConverter;

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:}")
    private String envUsername;

    @Value("${SPRING_DATASOURCE_PASSWORD:}")
    private String envPassword;

    public ProdDataSourceConfig(DatabaseUrlConverter urlConverter) {
        this.urlConverter = urlConverter;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            String errorMsg = "CRITICAL: DATABASE_URL environment variable is required in 'prod' profile but was not set.";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        DatabaseUrlConverter.ParsedDatabaseUrl parsed = urlConverter.convert(databaseUrl);

        String username = (envUsername != null && !envUsername.isBlank()) ? envUsername : parsed.username();
        String password = (envPassword != null && !envPassword.isBlank()) ? envPassword : parsed.password();

        log.info("Configuring production PostgreSQL DataSource: host={}, port={}, db={}, username={}",
                parsed.host(), parsed.port(), parsed.databaseName(), username);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(parsed.jdbcUrl());
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // Cloud resiliency settings
        config.setConnectionTimeout(10000); // 10 seconds
        config.setInitializationFailTimeout(15000); // 15 seconds before giving up on boot
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setPoolName("DatasetProdHikariPool");

        try {
            return new HikariDataSource(config);
        } catch (Exception e) {
            log.error("Failed to establish initial PostgreSQL connection to {}:{}. Verify that the database is active and reachable: {}",
                    parsed.host(), parsed.port(), e.getMessage());
            throw e;
        }
    }
}
