package com.assignment.dataset;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DatasetIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private String getBaseUrl(String datasetName) {
        return "http://localhost:" + port + "/api/dataset/" + datasetName;
    }

    @Test
    @DisplayName("End-to-End: Should insert 3 sample records and verify records, group-by, and sort-by responses")
    void should_insert_three_sample_records_and_query_with_group_by_and_sort_by_e2e() throws Exception {
        String dataset = "company_staff";

        // 1. Insert 3 sample records
        String r1 = "{\"id\": 1, \"name\": \"John Doe\", \"age\": 30, \"department\": \"Engineering\"}";
        String r2 = "{\"id\": 2, \"name\": \"Jane Smith\", \"age\": 25, \"department\": \"Marketing\"}";
        String r3 = "{\"id\": 3, \"name\": \"Bob Johnson\", \"age\": 35, \"department\": \"Engineering\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> postRes1 = restTemplate.postForEntity(getBaseUrl(dataset) + "/record", new HttpEntity<>(r1, headers), String.class);
        assertThat(postRes1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode json1 = objectMapper.readTree(postRes1.getBody());
        assertThat(json1.get("message").asText()).isEqualTo("Record added successfully");
        assertThat(json1.get("dataset").asText()).isEqualTo(dataset);
        assertThat(json1.get("recordId").asLong()).isPositive();

        ResponseEntity<String> postRes2 = restTemplate.postForEntity(getBaseUrl(dataset) + "/record", new HttpEntity<>(r2, headers), String.class);
        assertThat(postRes2.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> postRes3 = restTemplate.postForEntity(getBaseUrl(dataset) + "/record", new HttpEntity<>(r3, headers), String.class);
        assertThat(postRes3.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. Query all records (no params) -> { "records": [ ... ] }
        ResponseEntity<String> allRes = restTemplate.getForEntity(getBaseUrl(dataset) + "/query", String.class);
        assertThat(allRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode allNode = objectMapper.readTree(allRes.getBody());
        assertThat(allNode.has("records")).isTrue();
        assertThat(allNode.has("sortedRecords")).isFalse();
        assertThat(allNode.has("groupedRecords")).isFalse();
        assertThat(allNode.get("records")).hasSize(3);
        // Verify internal "id" field in JSON is untouched
        assertThat(allNode.get("records").get(0).get("id").asInt()).isEqualTo(1);

        // 3. Query group-by department -> { "groupedRecords": { "Engineering": [2], "Marketing": [1] } }
        ResponseEntity<String> groupRes = restTemplate.getForEntity(getBaseUrl(dataset) + "/query?groupBy=department", String.class);
        assertThat(groupRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode groupNode = objectMapper.readTree(groupRes.getBody());
        assertThat(groupNode.has("groupedRecords")).isTrue();
        assertThat(groupNode.has("records")).isFalse();
        JsonNode eng = groupNode.get("groupedRecords").get("Engineering");
        JsonNode mkt = groupNode.get("groupedRecords").get("Marketing");
        assertThat(eng).hasSize(2);
        assertThat(mkt).hasSize(1);

        // 4. Query sort-by age ascending -> { "sortedRecords": [25, 30, 35] }
        ResponseEntity<String> sortAscRes = restTemplate.getForEntity(getBaseUrl(dataset) + "/query?sortBy=age&order=asc", String.class);
        assertThat(sortAscRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode sortAscNode = objectMapper.readTree(sortAscRes.getBody());
        assertThat(sortAscNode.has("sortedRecords")).isTrue();
        assertThat(sortAscNode.get("sortedRecords").get(0).get("age").asInt()).isEqualTo(25);
        assertThat(sortAscNode.get("sortedRecords").get(1).get("age").asInt()).isEqualTo(30);
        assertThat(sortAscNode.get("sortedRecords").get(2).get("age").asInt()).isEqualTo(35);

        // 5. Query sort-by age descending -> { "sortedRecords": [35, 30, 25] }
        ResponseEntity<String> sortDescRes = restTemplate.getForEntity(getBaseUrl(dataset) + "/query?sortBy=age&order=desc", String.class);
        assertThat(sortDescRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode sortDescNode = objectMapper.readTree(sortDescRes.getBody());
        assertThat(sortDescNode.get("sortedRecords").get(0).get("age").asInt()).isEqualTo(35);
        assertThat(sortDescNode.get("sortedRecords").get(1).get("age").asInt()).isEqualTo(30);
        assertThat(sortDescNode.get("sortedRecords").get(2).get("age").asInt()).isEqualTo(25);

        // 6. Query combined: group first by department, then sort inside each group by age ASC
        ResponseEntity<String> groupAndSortRes = restTemplate.getForEntity(
                getBaseUrl(dataset) + "/query?groupBy=department&sortBy=age&order=asc",
                String.class
        );
        assertThat(groupAndSortRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode combinedNode = objectMapper.readTree(groupAndSortRes.getBody());
        assertThat(combinedNode.has("groupedRecords")).isTrue();
        JsonNode sortedEng = combinedNode.get("groupedRecords").get("Engineering");
        assertThat(sortedEng.get(0).get("name").asText()).isEqualTo("John Doe"); // age 30
        assertThat(sortedEng.get(1).get("name").asText()).isEqualTo("Bob Johnson"); // age 35
    }

    @Test
    @DisplayName("Edge Cases: missing fields, null values, mixed types, non-existent dataset, invalid params")
    void should_verify_edge_cases_in_real_flow() throws Exception {
        String dataset = "edge_cases_dataset";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Insert records with missing and null values
        restTemplate.postForEntity(getBaseUrl(dataset) + "/record", new HttpEntity<>("{\"title\":\"Item A\",\"score\":50}", headers), String.class);
        restTemplate.postForEntity(getBaseUrl(dataset) + "/record", new HttpEntity<>("{\"title\":\"Item B\",\"score\":null}", headers), String.class);
        restTemplate.postForEntity(getBaseUrl(dataset) + "/record", new HttpEntity<>("{\"title\":\"Item C\"}", headers), String.class);
        restTemplate.postForEntity(getBaseUrl(dataset) + "/record", new HttpEntity<>("{\"title\":\"Item D\",\"score\":10}", headers), String.class);

        // Verify nulls-last sorting
        ResponseEntity<String> sortRes = restTemplate.getForEntity(getBaseUrl(dataset) + "/query?sortBy=score&order=asc", String.class);
        assertThat(sortRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode sorted = objectMapper.readTree(sortRes.getBody()).get("sortedRecords");
        assertThat(sorted.get(0).get("title").asText()).isEqualTo("Item D"); // score 10
        assertThat(sorted.get(1).get("title").asText()).isEqualTo("Item A"); // score 50
        // items B and C should be last
        assertThat(sorted.get(2).get("score").isNull()).isTrue();
        assertThat(sorted.get(3).has("score")).isFalse();

        // Query non-existent dataset -> 404
        ResponseEntity<String> notFoundRes = restTemplate.getForEntity(getBaseUrl("does_not_exist") + "/query", String.class);
        assertThat(notFoundRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode err404 = objectMapper.readTree(notFoundRes.getBody());
        assertThat(err404.get("status").asInt()).isEqualTo(404);
        assertThat(err404.get("error").asText()).isEqualTo("Not Found");

        // Query with non-existent field -> 400
        ResponseEntity<String> fieldNotFoundRes = restTemplate.getForEntity(getBaseUrl(dataset) + "/query?sortBy=non_existent_field", String.class);
        assertThat(fieldNotFoundRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode errField = objectMapper.readTree(fieldNotFoundRes.getBody());
        assertThat(errField.get("status").asInt()).isEqualTo(400);
        assertThat(errField.get("error").asText()).isEqualTo("Bad Request");

        // Query with invalid order -> 400
        ResponseEntity<String> invalidOrderRes = restTemplate.getForEntity(getBaseUrl(dataset) + "/query?sortBy=score&order=sideways", String.class);
        assertThat(invalidOrderRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode errOrder = objectMapper.readTree(invalidOrderRes.getBody());
        assertThat(errOrder.get("status").asInt()).isEqualTo(400);

        // Insert empty body -> 400
        ResponseEntity<String> emptyBodyRes = restTemplate.postForEntity(getBaseUrl(dataset) + "/record", new HttpEntity<>("", headers), String.class);
        assertThat(emptyBodyRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Insert non-object JSON -> 400
        ResponseEntity<String> arrayBodyRes = restTemplate.postForEntity(getBaseUrl(dataset) + "/record", new HttpEntity<>("[1,2,3]", headers), String.class);
        assertThat(arrayBodyRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
