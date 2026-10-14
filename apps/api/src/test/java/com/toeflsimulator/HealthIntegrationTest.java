package com.toeflsimulator;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthIntegrationTest {
    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate jdbc;
    @Test void readinessRequiresMigratedDatabase() {
        var response = http.getForEntity("/actuator/health/readiness", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("UP");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.schemata WHERE schema_name='simulator'", Integer.class)).isEqualTo(1);
    }
    @Test void healthDoesNotDiscloseDatabaseDetails() {
        var health = http.getForObject("/actuator/health", JsonNode.class);
        assertThat(health.path("status").asText()).isEqualTo("UP");
        assertThat(health.has("components")).isFalse();
        assertThat(health.toString()).doesNotContain("jdbc:", "password", "database", "details");
        assertThat(http.getForEntity("/api/v1/sessions", String.class).getStatusCode().value()).isEqualTo(401);
    }
}
