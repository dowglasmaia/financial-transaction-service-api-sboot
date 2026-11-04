package com.dowglasmaia.financialtransaction.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.dowglasmaia.financialtransaction.support.MockIssuer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * BE-08: a complete flow (deposit, withdrawal, recharge, Pix, and the refused ones) leaves NO account number, agency, Pix
 * key, phone number or token in any log line, including the exception messages that are logged.
 */
class LogHygieneTest extends AbstractIntegrationTest {

    private static final String[] FORBIDDEN = {"2022", "0211", "52998224725", "529.982.247-25", "912345678", "11912345678", "maria@example.com", "Bearer ", "eyJ"};

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

    private List<String> everyLoggedText() {
        List<String> texts = new ArrayList<>();
        for (ILoggingEvent event : new ArrayList<>(appender.list)) {
            texts.add(event.getFormattedMessage());
            for (IThrowableProxy t = event.getThrowableProxy(); t != null; t = t.getCause()) {
                texts.add(String.valueOf(t.getMessage()));
            }
        }
        return texts;
    }

    @Test
    void aFullFlowLogsNoPersonalData() throws Exception {
        post("/deposit", "{\"agency\":\"0211\",\"account\":\"2022\",\"amount\":100.00}").andExpect(status().isNoContent());
        post("/atm-withdrawal", "{\"amount\":10.00}").andExpect(status().isNoContent());
        post("/recharge-mobile", "{\"phoneNumber\":912345678,\"areaCode\":11,\"carrier\":\"VIVO\",\"amount\":10.00}").andExpect(status().isNoContent());
        post("/send-pix", "{\"pixKey\":\"52998224725\",\"keyType\":\"cpf\",\"transactionAmount\":10.00}").andExpect(status().isNoContent());
        // the refused ones log too
        post("/send-pix", "{\"pixKey\":\"11111111111\",\"keyType\":\"cpf\",\"transactionAmount\":10.00}").andExpect(status().isUnprocessableEntity());
        post("/atm-withdrawal", "{\"amount\":999999.00}").andExpect(status().isUnprocessableEntity());
        post("/deposit", "{\"agency\":\"0211\",\"account\":\"0000\",\"amount\":10.00}").andExpect(status().isNotFound());
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/transactions?from=2026-03-11&to=2026-03-11")
              .header("Authorization", "Bearer " + MockIssuer.ownerToken()).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

        assertThat(appender.list).as("the flow logged something").isNotEmpty();
        for (String text : everyLoggedText()) {
            for (String forbidden : FORBIDDEN) {
                assertThat(text).as("log line must not contain " + forbidden).doesNotContain(forbidden);
            }
        }
    }
}
