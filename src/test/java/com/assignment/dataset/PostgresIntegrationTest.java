package com.assignment.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration test against a real PostgreSQL instance (via Testcontainers).
 *
 * <p>This test is tagged {@code "postgres"} and uses
 * {@code @Testcontainers(disabledWithoutDocker = true)}, so it is <em>skipped</em>
 * (not failed) when Docker is not available on the host.
 *
 * <p>It verifies:
 * <ul>
 *   <li>Insert records via POST /api/dataset/{dataset}/record</li>
 *   <li>GroupBy query returns correctly grouped records</li>
 *   <li>SortBy query returns records in ascending order</li>
 *   <li>The {@code record_data} column is stored as TEXT (no OID/CLOB issue)</li>
 * </ul>
 *
 * <p>The test spins up {@code postgres:16-alpine}, overrides the Spring datasource
 * via {@link DynamicPropertySource}, and activates the {@code prod} profile to exercise
 * the same {@link com.assignment.dataset.config.ProdDataSourceConfig} used in production.
 */
@Tag("postgres")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
class PostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("testuser")
                    .withPassword("testpass");

    /**
     * Override the DATABASE_URL that ProdDataSourceConfig reads so it points
     * at the Testcontainers instance.  The JDBC URL that Testcontainers provides
     * starts with {@code jdbc:postgresql://} which the converter accepts directly.
     */
    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        registry.add("DATABASE_URL", POSTGRES::getJdbcUrl);
        registry.add("SPRING_DATASOURCE_USERNAME", POSTGRES::getUsername);
        registry.add("SPRING_DATASOURCE_PASSWORD", POSTGRES::getPassword);
        // Disable sample-data seeder so it doesn't interfere with test assertions
        registry.add("SEED_SAMPLE_DATA", () -> "false");
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ObjectMapper objectMapper;

    // ── helpers ──────────────────────────────────────────────────────────────

    private String base() {
        return "http://localhost:" + port;
    }

    private ResponseEntity<Map> post(String dataset, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(
                base() + "/api/dataset/" + dataset + "/record",
                new HttpEntity<>(body, headers),
                Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> query(String dataset, String queryString) {
        return rest.getForObject(
                base() + "/api/dataset/" + dataset + "/query?" + queryString,
                Map.class);
    }

    // ── tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Insert three employee records and verify 201 Created responses")
    void should_insert_records_successfully() {
        ResponseEntity<Map> r1 = post("pg_employees",
                Map.of("name", "Alice", "age", 30, "department", "Engineering"));
        ResponseEntity<Map> r2 = post("pg_employees",
                Map.of("name", "Bob", "age", 25, "department", "Marketing"));
        ResponseEntity<Map> r3 = post("pg_employees",
                Map.of("name", "Carol", "age", 35, "department", "Engineering"));

        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(r3.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Response body must contain expected keys
        assertThat(r1.getBody()).containsKeys("message", "dataset", "recordId");
        assertThat(r1.getBody().get("dataset")).isEqualTo("pg_employees");
    }

    @Test
    @DisplayName("GroupBy department returns correctly grouped records")
    void should_group_by_department() {
        // Insert fresh records into an isolated dataset name
        post("pg_groupby", Map.of("name", "Alice", "age", 30, "department", "Engineering"));
        post("pg_groupby", Map.of("name", "Bob",   "age", 25, "department", "Marketing"));
        post("pg_groupby", Map.of("name", "Carol", "age", 35, "department", "Engineering"));

        Map<String, Object> response = query("pg_groupby", "groupBy=department");

        assertThat(response).containsKey("groupedRecords");
        @SuppressWarnings("unchecked")
        Map<String, List<?>> groups = (Map<String, List<?>>) response.get("groupedRecords");

        assertThat(groups).containsKeys("Engineering", "Marketing");
        assertThat(groups.get("Engineering")).hasSize(2);
        assertThat(groups.get("Marketing")).hasSize(1);
    }

    @Test
    @DisplayName("SortBy age ascending returns records in numeric order")
    void should_sort_by_age_ascending() {
        post("pg_sortby", Map.of("name", "Alice", "age", 30, "department", "Engineering"));
        post("pg_sortby", Map.of("name", "Bob",   "age", 25, "department", "Marketing"));
        post("pg_sortby", Map.of("name", "Carol", "age", 35, "department", "Engineering"));

        Map<String, Object> response = query("pg_sortby", "sortBy=age&order=asc");

        assertThat(response).containsKey("sortedRecords");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sorted = (List<Map<String, Object>>) response.get("sortedRecords");

        assertThat(sorted).hasSize(3);
        // Ages must be in ascending order: 25, 30, 35
        List<Integer> ages = sorted.stream()
                .map(r -> ((Number) r.get("age")).intValue())
                .toList();
        assertThat(ages).containsExactly(25, 30, 35);
    }

    @Test
    @DisplayName("record_data TEXT column stores and retrieves JSON without OID or CLOB issues")
    void should_store_and_retrieve_json_as_text() {
        // Insert a record with nested and special-character fields to stress TEXT storage
        Map<String, Object> complexRecord = Map.of(
                "name", "Test User",
                "details", "Some text with 'quotes' and special chars: <>&",
                "score", 99.5,
                "active", true
        );
        ResponseEntity<Map> insertResponse = post("pg_text_col", complexRecord);
        assertThat(insertResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Retrieve and confirm all fields survived the round-trip
        Map<String, Object> queryResponse = query("pg_text_col", "");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) queryResponse.get("records");

        assertThat(records).hasSize(1);
        Map<String, Object> retrieved = records.get(0);
        assertThat(retrieved.get("name")).isEqualTo("Test User");
        assertThat(retrieved.get("details")).asString().contains("quotes");
        assertThat(retrieved.get("active")).isEqualTo(true);
    }
}
