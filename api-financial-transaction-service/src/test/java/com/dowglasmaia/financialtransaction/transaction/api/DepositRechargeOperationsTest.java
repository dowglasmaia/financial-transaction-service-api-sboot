package com.dowglasmaia.financialtransaction.transaction.api;

import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.dowglasmaia.financialtransaction.support.MockIssuer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SL-04: operation-specific rules of deposit and mobile recharge that the web pages rely on: the deposit shows in the
 * statement of whoever receives it, a replayed {@code Idempotency-Key} never credits or debits twice, and a recharge with
 * an unusable phone or carrier is a 422 that moves nothing (never a 500).
 */
class DepositRechargeOperationsTest extends AbstractIntegrationTest {

    private static final String DEPOSIT_OWN = "{\"agency\":\"0211\",\"account\":\"2022\",\"amount\":250.00}";
    private static final String RECHARGE = "{\"phoneNumber\":912345678,\"areaCode\":11,\"carrier\":\"VIVO\",\"amount\":30.00}";

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clean() {
        jdbc.update("delete from idempotency_key");
    }

    private ResultActions send(String token, String path, String key, String body) throws Exception {
        var request = MockMvcRequestBuilders.post("/api/v1/transactions" + path)
              .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).content(body)
              .header("Authorization", "Bearer " + token);
        if (key != null) request.header("Idempotency-Key", key);
        return mvc.perform(request);
    }

    /** The statement of today in the business time zone (single day: SL-02). */
    private static String today() {
        LocalDate day = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        return "?from=" + day + "&to=" + day;
    }

    // ---------------- Deposit ----------------

    @Test
    void aDepositToTheOwnAccountAppearsAsOneDepositRowInTheStatement() throws Exception {
        post("/deposit", DEPOSIT_OWN).andExpect(status().isNoContent());

        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.add(new BigDecimal("250.00")));
        JsonNode statement = bodyOf(get(today()).andExpect(status().isOk()));
        assertThat(statement.get("transaction")).hasSize(1);
        assertThat(statement.get("transaction").get(0).get("transactionType").asText()).isEqualTo("DEPOSIT");
        assertThat(statement.get("transaction").get(0).get("amount").decimalValue()).isEqualByComparingTo("250");
    }

    @Test
    void aThirdPartyDepositIsCreditedAndShowsInTheReceiversStatementNotTheSenders() throws Exception {
        createOtherAccount("3033", new BigDecimal("100.00"), MockIssuer.OTHER_SUB);

        post("/deposit", "{\"agency\":\"0211\",\"account\":\"3033\",\"amount\":50.00}").andExpect(status().isNoContent());

        assertThat(balanceOf("3033")).isEqualByComparingTo("150.00");
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE);
        JsonNode receiver = bodyOf(getAs(MockIssuer.tokenFor(MockIssuer.OTHER_SUB), today()).andExpect(status().isOk()));
        assertThat(receiver.get("transaction")).hasSize(1);
        assertThat(receiver.get("transaction").get(0).get("transactionType").asText()).isEqualTo("DEPOSIT");
        assertThat(bodyOf(get(today()).andExpect(status().isOk())).get("transaction")).isEmpty();
    }

    @Test
    void replayingADepositKeyCreditsOnce() throws Exception {
        send(MockIssuer.ownerToken(), "/deposit", "dep-1", DEPOSIT_OWN).andExpect(status().isNoContent());
        send(MockIssuer.ownerToken(), "/deposit", "dep-1", DEPOSIT_OWN)
              .andExpect(status().isNoContent()).andExpect(header().string("Idempotent-Replayed", "true"));

        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.add(new BigDecimal("250.00")));
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void anUnknownAccountIsA404ThatMovesNothingAndFreesTheKey() throws Exception {
        send(MockIssuer.ownerToken(), "/deposit", "dep-404", "{\"agency\":\"0211\",\"account\":\"0000\",\"amount\":50.00}")
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.code").value("NOT_FOUND"))
              .andExpect(jsonPath("$.message").value("Account not found."));
        assertThat(transactionRepository.count()).isZero();

        send(MockIssuer.ownerToken(), "/deposit", "dep-404", DEPOSIT_OWN).andExpect(status().isNoContent());
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    // ---------------- Recharge ----------------

    @Test
    void replayingARechargeKeyDebitsOnceAndWritesOneLedgerRowAndOneOutboxEvent() throws Exception {
        send(MockIssuer.ownerToken(), "/recharge-mobile", "rec-1", RECHARGE).andExpect(status().isNoContent());
        send(MockIssuer.ownerToken(), "/recharge-mobile", "rec-1", RECHARGE)
              .andExpect(status().isNoContent()).andExpect(header().string("Idempotent-Replayed", "true"));

        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.subtract(new BigDecimal("30.00")));
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.count()).isEqualTo(1);
    }

    @Test
    void aRechargeIsOneDebitOneLedgerRowAndOneOutboxEvent() throws Exception {
        post("/recharge-mobile", RECHARGE).andExpect(status().isNoContent());

        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE.subtract(new BigDecimal("30.00")));
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.count()).isEqualTo(1);
    }

    @ParameterizedTest(name = "phone {0} with area code {1} is refused")
    @ValueSource(strings = {"12345678:11", "812345678:11", "912345678:0", "912345678:1", "91234567:11"})
    void anUnusablePhoneIsA422ThatMovesNothing(String pair) throws Exception {
        String[] parts = pair.split(":");
        post("/recharge-mobile", "{\"phoneNumber\":" + parts[0] + ",\"areaCode\":" + parts[1] + ",\"carrier\":\"TIM\",\"amount\":20}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Invalid cell phone numer"));
        assertNothingMoved();
    }

    @Test
    void aPhoneTooBigForTheContractIsA422ThatMovesNothing() throws Exception {
        post("/recharge-mobile", "{\"phoneNumber\":9123456789,\"areaCode\":11,\"carrier\":\"TIM\",\"amount\":20}")
              .andExpect(status().isUnprocessableEntity());
        assertNothingMoved();
    }

    @ParameterizedTest(name = "carrier {0} is refused")
    @ValueSource(strings = {"OI", "vivo", "", "CLARO ", "NEXTEL"})
    void anUnknownCarrierIsA422ThatMovesNothing(String carrier) throws Exception {
        post("/recharge-mobile", "{\"phoneNumber\":912345678,\"areaCode\":11,\"carrier\":\"" + carrier + "\",\"amount\":20}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"));
        assertNothingMoved();
    }

    @ParameterizedTest(name = "amount {0} is refused")
    @ValueSource(strings = {"0", "-5", "0.00", "-0.01", "\"abc\""})
    void anUnusableAmountIsA422ThatMovesNothing(String amount) throws Exception {
        post("/recharge-mobile", "{\"phoneNumber\":912345678,\"areaCode\":11,\"carrier\":\"CLARO\",\"amount\":" + amount + "}")
              .andExpect(status().isUnprocessableEntity());
        assertNothingMoved();
    }

    @Test
    void everyCarrierOfTheContractIsAccepted() throws Exception {
        for (String carrier : new String[]{"VIVO", "CLARO", "TIM", "ALGAR"}) {
            post("/recharge-mobile", "{\"phoneNumber\":912345678,\"areaCode\":11,\"carrier\":\"" + carrier + "\",\"amount\":10}")
                  .andExpect(status().isNoContent());
        }
        assertThat(outboxEventRepository.count()).isEqualTo(4);
    }

    private void assertNothingMoved() {
        assertThat(balanceOf(SEED_ACCOUNT)).isEqualByComparingTo(SEED_BALANCE);
        assertThat(transactionRepository.count()).isZero();
        assertThat(outboxEventRepository.count()).isZero();
    }
}
