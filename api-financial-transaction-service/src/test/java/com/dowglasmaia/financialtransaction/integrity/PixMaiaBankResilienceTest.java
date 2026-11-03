package com.dowglasmaia.financialtransaction.integrity;

import com.dowglasmaia.financialtransaction.account.infrastructure.persistence.AccountEntity;
import com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence.TransactionEntity;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.account.infrastructure.persistence.AccountJpaRepository;
import com.dowglasmaia.financialtransaction.repository.OutboxEventRepository;
import com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence.TransactionJpaRepository;
import com.dowglasmaia.financialtransaction.support.MockIssuer;
import com.dowglasmaia.financialtransaction.support.SharedPostgres;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.List;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec BE-03: the REAL Feign client against a stub MaiaBank (no mocks): HTTP 500 and read timeouts compensate the
 * reservation, and the circuit breaker really opens after repeated failures (it stops calling MaiaBank).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "recharge-mobile-topic")
@TestPropertySource(properties = {
      "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
      "spring.cloud.openfeign.client.config.default.read-timeout=500",
      "spring.cloud.openfeign.client.config.default.connect-timeout=500"
})
class PixMaiaBankResilienceTest {

    private static final String SEED_ACCOUNT = "2022";
    private static final BigDecimal SEED_BALANCE = new BigDecimal("5000.00");
    private static final String PIX = "{\"pixKey\":\"82215344040\",\"keyType\":\"cpf\",\"transactionAmount\":100.00}";

    private static final AtomicInteger HITS = new AtomicInteger();
    private static final AtomicInteger IN_FLIGHT = new AtomicInteger();
    private static volatile int responseStatus = 200;
    private static volatile long delayMillis = 0;
    private static final HttpServer MAIABANK = startStub();

    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/transactions", exchange -> {
                HITS.incrementAndGet();
                IN_FLIGHT.incrementAndGet();
                try {
                    if (delayMillis > 0) Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                try {
                    exchange.sendResponseHeaders(responseStatus, -1);
                } catch (IOException ignored) {
                    // the client already gave up (timeout)
                } finally {
                    exchange.close();
                    IN_FLIGHT.decrementAndGet();
                }
            });
            server.setExecutor(Executors.newCachedThreadPool()); // a slow request must not block the next one
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
        MockIssuer.register(registry);
        registry.add("api.client.maiabank.url", () -> "http://127.0.0.1:" + MAIABANK.getAddress().getPort() + "/api/v1/transactions");
    }

    @AfterAll
    static void stopStub() {
        MAIABANK.stop(0);
    }

    @Autowired MockMvc mvc;
    @Autowired AccountJpaRepository accounts;
    @Autowired TransactionJpaRepository ledger;
    @Autowired OutboxEventRepository outbox;
    @Autowired CircuitBreakerRegistry breakers;

    @BeforeEach
    void reset() {
        await().atMost(Duration.ofSeconds(10)).until(() -> IN_FLIGHT.get() == 0); // leftovers of a slow test
        ledger.deleteAll();
        outbox.deleteAll();
        AccountEntity seed = accounts.findByNumber(SEED_ACCOUNT).orElseThrow();
        seed.setBalance(SEED_BALANCE);
        accounts.save(seed);
        HITS.set(0);
        responseStatus = 200;
        delayMillis = 0;
        breaker().reset();
    }

    private CircuitBreaker breaker() {
        return breakers.circuitBreaker("maiabank");
    }

    private org.springframework.test.web.servlet.ResultActions pix() throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/v1/transactions/send-pix")
              .header("Authorization", "Bearer " + MockIssuer.ownerToken())
              .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(PIX));
    }

    private BigDecimal balance() {
        return accounts.findByNumber(SEED_ACCOUNT).orElseThrow().getBalance();
    }

    @Test
    void aSuccessfulPixDebitsAndConfirmsTheLedger() throws Exception {
        pix().andExpect(status().isNoContent());

        assertThat(balance()).isEqualByComparingTo("4900.00");
        List<TransactionEntity> rows = ledger.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(HITS.get()).isEqualTo(1);
    }

    @Test
    void anHttp500FromMaiaBankIsCompensated() throws Exception {
        responseStatus = 500;

        pix().andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value("Fail send Pix."));

        assertThat(balance()).as("debit reversed").isEqualByComparingTo(SEED_BALANCE);
        assertThat(ledger.findAll()).singleElement().extracting(TransactionEntity::getStatus).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void aReadTimeoutIsCompensated() throws Exception {
        delayMillis = 2500; // longer than the 500 ms read timeout configured for this test

        long started = System.currentTimeMillis();
        pix().andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value("Fail send Pix."));
        long elapsed = System.currentTimeMillis() - started;

        assertThat(elapsed).as("the request must not wait for the slow bank").isLessThan(2400);
        assertThat(balance()).isEqualByComparingTo(SEED_BALANCE);
        assertThat(ledger.findAll()).singleElement().extracting(TransactionEntity::getStatus).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void theCircuitBreakerOpensAfterRepeatedFailuresAndStopsCallingMaiaBank() throws Exception {
        responseStatus = 500;
        for (int i = 0; i < 6; i++) { // slidingWindowSize 6, failureRateThreshold 50%
            pix().andExpect(status().isUnprocessableEntity());
        }
        assertThat(HITS.get()).isEqualTo(6);
        assertThat(breaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);

        pix().andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.message").value("Fail send Pix."));

        assertThat(HITS.get()).as("an open breaker short-circuits: MaiaBank is not called").isEqualTo(6);
        assertThat(balance()).as("every reservation was compensated").isEqualByComparingTo(SEED_BALANCE);
        assertThat(ledger.findAll()).hasSize(7).allMatch(t -> t.getStatus() == TransactionStatus.FAILED);
    }
}
