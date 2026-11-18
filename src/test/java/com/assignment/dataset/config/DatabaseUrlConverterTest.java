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

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://dpg-c01234567-a.oregon-postgres.render.com:5432/appdb?sslmode=require");
        assertThat(result.username()).isEqualTo("appuser");
        assertThat(result.password()).isEqualTo("secret123");
        assertThat(result.host()).isEqualTo("dpg-c01234567-a.oregon-postgres.render.com");
        assertThat(result.port()).isEqualTo(5432);
        assertThat(result.databaseName()).isEqualTo("appdb");
    }

    @Test
    @DisplayName("Should convert postgresql:// scheme with default port 5432 when port is omitted")
    void should_convert_postgresql_url_with_missing_port() {
        String urlWithoutPort = "postgresql://myuser:mypass@dpg-render-internal/prod_dataset";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(urlWithoutPort);

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://dpg-render-internal:5432/prod_dataset");
        assertThat(result.username()).isEqualTo("myuser");
        assertThat(result.password()).isEqualTo("mypass");
        assertThat(result.host()).isEqualTo("dpg-render-internal");
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

    // ── Render SSL logic ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Internal Render URL (no dot in host) should NOT append sslmode")
    void should_not_add_ssl_for_internal_render_host() {
        String internalUrl = "postgres://user:pass@dpg-xxxx-a/dbname";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(internalUrl);

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://dpg-xxxx-a:5432/dbname");
        assertThat(result.jdbcUrl()).doesNotContain("sslmode");
    }

    @Test
    @DisplayName("External Render URL (dot in host) should append sslmode=require")
    void should_add_ssl_for_external_render_host() {
        String externalUrl = "postgres://user:pass@dpg-xxxx-a.oregon-postgres.render.com/dbname";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(externalUrl);

        assertThat(result.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://dpg-xxxx-a.oregon-postgres.render.com:5432/dbname?sslmode=require");
        assertThat(result.host()).isEqualTo("dpg-xxxx-a.oregon-postgres.render.com");
    }

    @Test
    @DisplayName("External Render URL with missing port should use default 5432 and append sslmode=require")
    void should_add_ssl_for_external_host_with_missing_port() {
        // Fix: Removed :5432 so this truly tests a missing port on external host
        String externalUrl = "postgres://user:pass@dpg-xxxx-a.oregon-postgres.render.com/appdb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(externalUrl);

        assertThat(result.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://dpg-xxxx-a.oregon-postgres.render.com:5432/appdb?sslmode=require");
        assertThat(result.port()).isEqualTo(5432);
    }

    @Test
    @DisplayName("If URL already contains sslmode parameter, keep it and do not append another one")
    void should_not_duplicate_sslmode_if_already_present() {
        String urlWithSsl = "postgres://user:pass@dpg-xxxx-a.oregon-postgres.render.com:5432/appdb?sslmode=verify-full";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(urlWithSsl);

        assertThat(result.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://dpg-xxxx-a.oregon-postgres.render.com:5432/appdb?sslmode=verify-full");
        assertThat(result.jdbcUrl()).doesNotContain("?sslmode=require");
        assertThat(result.jdbcUrl()).doesNotContain("&sslmode=require");
    }

    @Test
    @DisplayName("Do not add sslmode for localhost")
    void should_not_add_ssl_for_localhost() {
        String localUrl = "postgres://user:pass@localhost:5432/mydb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(localUrl);

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/mydb");
        assertThat(result.jdbcUrl()).doesNotContain("sslmode");
    }

    @Test
    @DisplayName("Do not add sslmode for IPv4 addresses (e.g. 127.0.0.1 or private IPs)")
    void should_not_add_ssl_for_ipv4_addresses() {
        String loopback = "postgres://user:pass@127.0.0.1:5432/mydb";
        DatabaseUrlConverter.ParsedDatabaseUrl r1 = converter.convert(loopback);
        assertThat(r1.jdbcUrl()).isEqualTo("jdbc:postgresql://127.0.0.1:5432/mydb");
        assertThat(r1.jdbcUrl()).doesNotContain("sslmode");

        String privateIp = "postgres://user:pass@192.168.1.50:5432/mydb";
        DatabaseUrlConverter.ParsedDatabaseUrl r2 = converter.convert(privateIp);
        assertThat(r2.jdbcUrl()).isEqualTo("jdbc:postgresql://192.168.1.50:5432/mydb");
        assertThat(r2.jdbcUrl()).doesNotContain("sslmode");
    }

    @Test
    @DisplayName("Do not add sslmode for IPv6 addresses (e.g. [::1])")
    void should_not_add_ssl_for_ipv6_addresses() {
        String ipv6Url = "postgres://user:pass@[::1]:5432/mydb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(ipv6Url);

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://[::1]:5432/mydb");
        assertThat(result.jdbcUrl()).doesNotContain("sslmode");
    }

    @Test
    @DisplayName("DB_SSL_MODE override should set sslmode when specified (e.g. disable on external host)")
    void should_honor_db_ssl_mode_override_disable() {
        DatabaseUrlConverter overrideConverter = new DatabaseUrlConverter("disable");
        String externalUrl = "postgres://user:pass@dpg-xxxx-a.oregon-postgres.render.com:5432/appdb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = overrideConverter.convert(externalUrl);

        assertThat(result.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://dpg-xxxx-a.oregon-postgres.render.com:5432/appdb?sslmode=disable");
    }

    @Test
    @DisplayName("DB_SSL_MODE override should set sslmode=require on internal host when requested")
    void should_honor_db_ssl_mode_override_require() {
        DatabaseUrlConverter overrideConverter = new DatabaseUrlConverter("require");
        String internalUrl = "postgres://user:pass@dpg-internal/appdb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = overrideConverter.convert(internalUrl);

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://dpg-internal:5432/appdb?sslmode=require");
    }

    @Test
    @DisplayName("jdbc:postgresql:// URL with external host should append sslmode=require if not present")
    void should_handle_external_jdbc_url() {
        String jdbcExternal = "jdbc:postgresql://dpg-xxxx-a.oregon-postgres.render.com:5432/appdb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(jdbcExternal);

        assertThat(result.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://dpg-xxxx-a.oregon-postgres.render.com:5432/appdb?sslmode=require");
        assertThat(result.host()).isEqualTo("dpg-xxxx-a.oregon-postgres.render.com");
    }

    @Test
    @DisplayName("jdbc:postgresql:// URL with existing sslmode should not duplicate it")
    void should_not_duplicate_sslmode_in_jdbc_url() {
        String jdbcWithSsl = "jdbc:postgresql://dpg-xxxx-a.oregon-postgres.render.com:5432/appdb?sslmode=prefer";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(jdbcWithSsl);

        assertThat(result.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://dpg-xxxx-a.oregon-postgres.render.com:5432/appdb?sslmode=prefer");
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("null input throws IllegalArgumentException with 'null or empty' message")
    void should_throw_when_input_is_null() {
        assertThatThrownBy(() -> converter.convert(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Database URL cannot be null or empty");
    }

    @Test
    @DisplayName("Missing database name should throw IllegalArgumentException")
    void should_throw_when_database_name_is_missing() {
        assertThatThrownBy(() -> converter.convert("postgres://user:pass@host:5432"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing database name");

        assertThatThrownBy(() -> converter.convert("postgres://user:pass@host:5432/"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing database name");
    }

    @Test
    @DisplayName("Non-numeric port should throw IllegalArgumentException")
    void should_throw_when_port_is_non_numeric() {
        assertThatThrownBy(() -> converter.convert("postgres://user:pass@host:abc/mydb"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid database URL format");
    }

    @Test
    @DisplayName("URL without credentials should parse host and database correctly")
    void should_handle_url_without_credentials() {
        String url = "postgres://dpg-internal-host/mydb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(url);

        assertThat(result.username()).isNull();
        assertThat(result.password()).isNull();
        assertThat(result.host()).isEqualTo("dpg-internal-host");
        assertThat(result.port()).isEqualTo(5432);
        assertThat(result.databaseName()).isEqualTo("mydb");
        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://dpg-internal-host:5432/mydb");
    }

    @Test
    @DisplayName("Password containing literal '+' must NOT turn into space")
    void should_preserve_literal_plus_in_password() {
        String urlWithPlus = "postgres://admin:pass+word@db-host:5432/testdb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(urlWithPlus);

        assertThat(result.password()).isEqualTo("pass+word");
        assertThat(result.password()).doesNotContain(" ");
    }

    @Test
    @DisplayName("Password containing encoded '%2B' must decode to '+'")
    void should_decode_encoded_plus_in_password() {
        String urlWithEncodedPlus = "postgres://admin:pass%2Bword@db-host:5432/testdb";

        DatabaseUrlConverter.ParsedDatabaseUrl result = converter.convert(urlWithEncodedPlus);

        assertThat(result.password()).isEqualTo("pass+word");
    }
}
