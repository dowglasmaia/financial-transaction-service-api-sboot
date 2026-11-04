package com.dowglasmaia.financialtransaction.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.dowglasmaia.financialtransaction.support.MockIssuer;
import com.dowglasmaia.financialtransaction.support.SharedPostgres;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * BE-08 on REAL servers: the actuator answers on its own port and never on the API port; Prometheus is scraped without a
 * token on the (private) management port, the rest of the actuator stays closed, and business endpoints are not there.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability // tests switch metric exporters off by default; Prometheus must be real here
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "recharge-mobile-topic")
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}", "management.server.port=0"})
class ManagementPortRealServerTest {

    @LocalServerPort int apiPort;
    @LocalManagementPort int managementPort;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        MockIssuer.register(registry);
    }

    private int status(int port, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private String body(int port, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    @Test
    void thePortsAreDifferent() {
        assertThat(managementPort).isNotEqualTo(apiPort);
    }

    @Test
    void healthAndPrometheusAnswerOnTheManagementPortWithoutAToken() throws Exception {
        assertThat(status(managementPort, "/actuator/health")).isEqualTo(200);
        assertThat(status(managementPort, "/actuator/prometheus")).isEqualTo(200);
        assertThat(body(managementPort, "/actuator/prometheus")).contains("jvm_memory_used_bytes");
    }

    @Test
    void theRestOfTheActuatorStaysClosedOnTheManagementPort() throws Exception {
        assertThat(status(managementPort, "/actuator/metrics")).isEqualTo(401);
    }

    @Test
    void theActuatorIsNotOnTheApiPort() throws Exception {
        assertThat(status(apiPort, "/actuator/prometheus")).isNotEqualTo(200);
        assertThat(status(apiPort, "/actuator/health")).isNotEqualTo(200);
    }

    @Test
    void businessEndpointsAreNotOnTheManagementPort() throws Exception {
        assertThat(status(managementPort, "/api/v1/accounts/me")).isNotEqualTo(200);
    }
}
