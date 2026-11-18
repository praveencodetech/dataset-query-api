package com.assignment.dataset.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.net.URI;
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
    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
    private static final Pattern SSLMODE_PARAM_PATTERN =
            Pattern.compile("(^|[&?])sslmode=");

    private final String sslModeOverride;

    /**
     * Default constructor for standalone instantiation.
     * Checks for the DB_SSL_MODE environment variable.
     */
    public DatabaseUrlConverter() {
        this(System.getenv("DB_SSL_MODE"));
    }

    /**
     * Spring-managed constructor accepting an optional DB_SSL_MODE property or env var.
     *
     * @param sslModeOverride optional SSL mode override (e.g. "disable", "require")
     */
    @Autowired
    public DatabaseUrlConverter(@Value("${DB_SSL_MODE:}") String sslModeOverride) {
        if (sslModeOverride != null && !sslModeOverride.isBlank()) {
            this.sslModeOverride = sslModeOverride.trim();
        } else {
            String env = System.getenv("DB_SSL_MODE");
            this.sslModeOverride = (env != null && !env.isBlank()) ? env.trim() : null;
        }
    }

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
            String rawUserInfo = uri.getRawUserInfo();
            if (rawUserInfo != null && !rawUserInfo.isEmpty()) {
                String[] parts = rawUserInfo.split(":", 2);
                username = UriUtils.decode(parts[0], StandardCharsets.UTF_8);
                if (parts.length > 1) {
                    password = UriUtils.decode(parts[1], StandardCharsets.UTF_8);
                }
            }

            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                    .append(host)
                    .append(":")
                    .append(port)
                    .append("/")
                    .append(databaseName);

            String existingQuery = uri.getQuery();
            if (existingQuery != null && !existingQuery.isBlank()) {
                jdbcUrl.append("?").append(existingQuery);
            }

            appendSslModeIfNeeded(jdbcUrl, host, existingQuery);

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
        String existingQuery = matcher.group("query");

        String finalJdbcUrl = jdbcUrl;
        String sslMode = determineSslModeToAppend(host, existingQuery);
        if (sslMode != null) {
            if (existingQuery != null && !existingQuery.isBlank()) {
                finalJdbcUrl = jdbcUrl + "&sslmode=" + sslMode;
            } else {
                finalJdbcUrl = jdbcUrl + "?sslmode=" + sslMode;
            }
        }

        return new ParsedDatabaseUrl(finalJdbcUrl, null, null, host, port, databaseName);
    }

    private void appendSslModeIfNeeded(StringBuilder jdbcUrl, String host, String existingQuery) {
        String sslMode = determineSslModeToAppend(host, existingQuery);
        if (sslMode != null) {
            if (existingQuery != null && !existingQuery.isBlank()) {
                jdbcUrl.append("&sslmode=").append(sslMode);
            } else {
                jdbcUrl.append("?sslmode=").append(sslMode);
            }
        }
    }

    private String determineSslModeToAppend(String host, String existingQuery) {
        // 1. If already contains sslmode, keep it as is
        if (hasSslMode(existingQuery)) {
            return null;
        }

        // 2. Explicit DB_SSL_MODE override
        if (sslModeOverride != null && !sslModeOverride.isBlank()) {
            return sslModeOverride;
        }

        // 3. Automatic behavior: add sslmode=require for external hosts (excluding IP addresses and localhost)
        if (host != null && host.contains(".") && !isLocalOrIpAddress(host)) {
            return "require";
        }

        return null;
    }

    private boolean hasSslMode(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        return SSLMODE_PARAM_PATTERN.matcher(query).find();
    }

    private boolean isLocalOrIpAddress(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        String cleanHost = host.trim().toLowerCase();
        if (cleanHost.startsWith("[") && cleanHost.endsWith("]")) {
            cleanHost = cleanHost.substring(1, cleanHost.length() - 1);
        }
        if ("localhost".equals(cleanHost) || "::1".equals(cleanHost) || cleanHost.contains(":")) {
            return true;
        }
        return IPV4_PATTERN.matcher(cleanHost).matches();
    }
}
