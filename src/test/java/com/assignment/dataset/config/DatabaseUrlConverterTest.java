package com.assignment.dataset.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseUrlConverterTest {

    private final DatabaseUrlConverter converter = new DatabaseUrlConverter();

    @Test
    @DisplayName("Should convert standard Render postgres:// URL with credentials and port")
    void should_convert_render_postgres_url() {
        String renderUrl = "postgres://appuser:secret123@dpg-c01234567-a.oregon-postgres.render.com:5432/appdb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(renderUrl);

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://dpg-c01234567-a.oregon-postgres.render.com:5432/appdb");
        assertThat(result.username()).isEqualTo("appuser");
        assertThat(result.password()).isEqualTo("secret123");
        assertThat(result.host()).isEqualTo("dpg-c01234567-a.oregon-postgres.render.com");
        assertThat(result.port()).isEqualTo(5432);
        assertThat(result.databaseName()).isEqualTo("appdb");
    }

    @Test
    @DisplayName("Should convert postgresql:// scheme with default port 5432 when port is omitted")
    void should_convert_postgresql_url_with_missing_port() {
        String urlWithoutPort = "postgresql://myuser:mypass@db.render.internal/prod_dataset";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(urlWithoutPort);

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://db.render.internal:5432/prod_dataset");
        assertThat(result.username()).isEqualTo("myuser");
        assertThat(result.password()).isEqualTo("mypass");
        assertThat(result.host()).isEqualTo("db.render.internal");
        assertThat(result.port()).isEqualTo(5432);
        assertThat(result.databaseName()).isEqualTo("prod_dataset");
    }

    @Test
    @DisplayName("Should accept already-formatted jdbc:postgresql:// URL")
    void should_accept_standard_jdbc_url() {
        String jdbcUrl = "jdbc:postgresql://localhost:5432/datasetdb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(jdbcUrl);

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/datasetdb");
        assertThat(result.username()).isNull();
        assertThat(result.password()).isNull();
        assertThat(result.host()).isEqualTo("localhost");
        assertThat(result.port()).isEqualTo(5432);
        assertThat(result.databaseName()).isEqualTo("datasetdb");
    }

    @Test
    @DisplayName("Should handle URL-encoded special characters in password correctly")
    void should_handle_special_characters_in_password() {
        // Password "p@ss:w0rd!" encoded as "p%40ss%3Aw0rd%21"
        String urlWithSpecialChars = "postgres://admin:p%40ss%3Aw0rd%21@db-host:5432/testdb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(urlWithSpecialChars);

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://db-host:5432/testdb");
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.password()).isEqualTo("p@ss:w0rd!");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException with clear message for invalid URLs")
    void should_throw_for_invalid_url() {
        assertThatThrownBy(() -> converter.convert("mysql://user:pass@host:3306/db"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported database URL scheme");

        assertThatThrownBy(() -> converter.convert(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Database URL cannot be null or empty");

        assertThatThrownBy(() -> converter.convert("postgres://[invalid-host:port/db"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid database URL format");
    }

    @Test
    @DisplayName("ParsedDatabaseUrl.toString() should never expose password in plain text")
    void should_never_log_or_expose_password_in_to_string() {
        String url = "postgres://user:verySecretPassword@host:5432/db";
        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(url);

        String output = result.toString();
        assertThat(output).doesNotContain("verySecretPassword");
        assertThat(output).contains("******");
    }
}
