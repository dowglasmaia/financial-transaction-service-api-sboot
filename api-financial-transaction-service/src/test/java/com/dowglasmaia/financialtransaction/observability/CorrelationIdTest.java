package com.dowglasmaia.financialtransaction.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.dowglasmaia.financialtransaction.support.MockIssuer;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** BE-08: every request has a correlation id (the caller's X-Request-Id when it is safe, else a new one): echoed and in the MDC. */
class CorrelationIdTest extends AbstractIntegrationTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger root;

    @BeforeEach
    void capture() {
        root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
    }

    @AfterEach
    void release() {
        root.detachAppender(appender);
    }

    private ResultActions statement(String requestId) throws Exception {
        var request = MockMvcRequestBuilders.get("/api/v1/transactions?from=2026-03-11&to=2026-03-11")
              .header("Authorization", "Bearer " + MockIssuer.ownerToken()).accept(MediaType.APPLICATION_JSON);
        if (requestId != null) request.header("X-Request-Id", requestId);
        return mvc.perform(request);
    }

    @Test
    void theCallersSafeIdIsEchoed() throws Exception {
        statement("trace-777.a_b").andExpect(status().isOk()).andExpect(header().string("X-Request-Id", "trace-777.a_b"));
    }

    @Test
    void withoutAnIdOneIsGenerated() throws Exception {
        String id = statement(null).andExpect(status().isOk()).andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(id).matches("[0-9a-f-]{36}");
    }

    @Test
    void anUnsafeIdIsReplacedNotEchoed() throws Exception {
        for (String unsafe : new String[]{"a b", "<script>", "x".repeat(65), "line\\nbreak"}) {
            String id = statement(unsafe).andExpect(status().isOk()).andReturn().getResponse().getHeader("X-Request-Id");
            assertThat(id).as(unsafe).matches("[0-9a-f-]{36}");
        }
    }

    @Test
    void theIdIsInTheLogContextOfTheRequest() throws Exception {
        post("/send-pix", "{\"pixKey\":\"11111111111\",\"keyType\":\"cpf\",\"transactionAmount\":10.00}");
        var request = MockMvcRequestBuilders.post("/api/v1/transactions/send-pix").header("X-Request-Id", "trace-log-1")
              .header("Authorization", "Bearer " + MockIssuer.ownerToken()).contentType(MediaType.APPLICATION_JSON)
              .content("{\"pixKey\":\"11111111111\",\"keyType\":\"cpf\",\"transactionAmount\":10.00}");
        mvc.perform(request).andExpect(status().isUnprocessableEntity());

        assertThat(new ArrayList<>(appender.list)).anyMatch(event -> "trace-log-1".equals(event.getMDCPropertyMap().get("requestId")));
    }
}
