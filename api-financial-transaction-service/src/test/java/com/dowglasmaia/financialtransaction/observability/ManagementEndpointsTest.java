package com.dowglasmaia.financialtransaction.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * BE-08: the actuator listens on its OWN port (never the API port). Prometheus scraping is open on that private port; the
 * rest of the actuator stays closed (see AuthenticationTest) and business endpoints are never reachable from it.
 */
class ManagementEndpointsTest extends AbstractIntegrationTest {

    @Value("${management.server.port}") String managementPort;

    @Test
    void theManagementPortIsNotTheApiPort() {
        assertThat(managementPort).isNotBlank().isNotEqualTo("8088");
    }

    @Test
    void prometheusCanBeScrapedWithoutATokenOnTheManagementPort() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/actuator/prometheus")).andExpect(status().isOk());
    }

    @Test
    void healthDoesNotLeakDetailsToAnonymousCallers() throws Exception {
        String body = mvc.perform(MockMvcRequestBuilders.get("/actuator/health")).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("jdbc:").doesNotContain("password");
    }
}
