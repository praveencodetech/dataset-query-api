package com.assignment.dataset.config;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts platform-provided database URLs (e.g., Render's postgres:// or postgresql://)
 * into standard JDBC URLs and extracts connection credentials.
 */
@Component
public class DatabaseUrlConverter {

    private static final int DEFAULT_POSTGRES_PORT = 5432;
    private static final Pattern JDBC_POSTGRES_PATTERN =
            Pattern.compile("^jdbc:postgresql://(?<host>[^:/]+)(?::(?<port>\\d+))?/(?<db>[^?]+)(?:\\?(?<query>.*))?$");

    /**
     * Parsed representation of a database URL.
     * Overrides toString() to ensure passwords are never exposed in log output.
     */
    public record ParsedDatabaseUrl(
            String jdbcUrl,
            String username,
            String password,
            String host,
            int port,
            String databaseName
    ) {
        @Override
        public String toString() {
            String maskedPass = (password != null && !password.isEmpty()) ? "******" : null;
            return "ParsedDatabaseUrl[jdbcUrl=" + jdbcUrl
                    + ", username=" + username
                    + ", password=" + maskedPass
                    + ", host=" + host
                    + ", port=" + port
                    + ", databaseName=" + databaseName + "]";
        }
    }

    /**
     * Converts a database URL string into {@link ParsedDatabaseUrl}.
     *
     * @param databaseUrl platform or JDBC database URL
     * @return parsed database connection details
     * @throws IllegalArgumentException if the URL is null, empty, unsupported, or invalid
     */
    public ParsedDatabaseUrl convert(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Database URL cannot be null or empty.");
        }

        String trimmed = databaseUrl.trim();

        // 1. Standard JDBC URL format
        if (trimmed.startsWith("jdbc:postgresql://")) {
            return parseJdbcUrl(trimmed);
        }

        // 2. Platform URI format (postgres:// or postgresql://)
        if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
            return parsePlatformUri(trimmed);
        }

        throw new IllegalArgumentException(
                "Unsupported database URL scheme. Expected 'postgres://', 'postgresql://', or 'jdbc:postgresql://'. Received: " + trimmed
        );
    }

    private ParsedDatabaseUrl parsePlatformUri(String rawUri) {
        URI uri;
        try {
            uri = new URI(rawUri);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid database URL format: " + rawUri, e);
        }

        try {
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Invalid database URL format: missing host in " + rawUri);
            }

            int port = (uri.getPort() > 0) ? uri.getPort() : DEFAULT_POSTGRES_PORT;

            String path = uri.getPath();
            if (path == null || path.length() <= 1) {
                throw new IllegalArgumentException("Invalid database URL format: missing database name in " + rawUri);
            }
            String databaseName = path.startsWith("/") ? path.substring(1) : path;

            String username = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                String[] parts = userInfo.split(":", 2);
                username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                if (parts.length > 1) {
                    password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }

            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                    .append(host)
                    .append(":")
                    .append(port)
                    .append("/")
                    .append(databaseName);

            if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                jdbcUrl.append("?").append(uri.getQuery());
            }

            return new ParsedDatabaseUrl(jdbcUrl.toString(), username, password, host, port, databaseName);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid database URL format: " + rawUri, e);
        }
    }

    private ParsedDatabaseUrl parseJdbcUrl(String jdbcUrl) {
        Matcher matcher = JDBC_POSTGRES_PATTERN.matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid JDBC PostgreSQL URL format: " + jdbcUrl);
        }

        String host = matcher.group("host");
        String portStr = matcher.group("port");
        int port = (portStr != null) ? Integer.parseInt(portStr) : DEFAULT_POSTGRES_PORT;
        String databaseName = matcher.group("db");

        return new ParsedDatabaseUrl(jdbcUrl, null, null, host, port, databaseName);
    }
}
