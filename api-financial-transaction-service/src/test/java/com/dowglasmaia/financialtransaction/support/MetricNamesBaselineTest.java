package com.dowglasmaia.financialtransaction.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * BE-07: "metric names unchanged" across the platform upgrade (Boot 3.2 -> 3.5, Java 17 -> 21 -> 25). The names below were
 * recorded on Boot 3.2.5 / Java 17; dashboards and alerts depend on them, so an upgrade may add metrics but never rename these.
 */
class MetricNamesBaselineTest extends AbstractIntegrationTest {

    private static final Set<String> BASELINE = Set.of(
          "http.server.requests",
          "jvm.memory.used",
          "jvm.memory.max",
          "jvm.gc.pause",
          "jvm.threads.live",
          "jvm.classes.loaded",
          "process.cpu.usage",
          "hikaricp.connections.active",
          "hikaricp.connections.max",
          "logback.events",
          "resilience4j.circuitbreaker.state",
          "resilience4j.circuitbreaker.calls",
          "resilience4j.circuitbreaker.failure.rate",
          "spring.kafka.template",
          "spring.security.filterchains");

    @Autowired MeterRegistry registry;

    @Test
    void theNamesDashboardsDependOnStillExist() throws Exception {
        get("?from=2026-03-11&to=2026-03-11").andExpect(status().isOk()); // makes http.server.requests appear

        Set<String> names = new TreeSet<>();
        for (Meter meter : registry.getMeters()) {
            names.add(meter.getId().getName());
        }
        assertThat(names).containsAll(BASELINE);
    }
}
