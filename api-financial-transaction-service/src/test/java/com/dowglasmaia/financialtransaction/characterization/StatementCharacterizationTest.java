package com.dowglasmaia.financialtransaction.characterization;

import com.dowglasmaia.financialtransaction.account.infrastructure.persistence.AccountEntity;
import com.dowglasmaia.financialtransaction.entity.Transaction;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Characterizes CURRENT behaviour of the statement and transaction lookup (spec BE-01). */
class StatementCharacterizationTest extends AbstractIntegrationTest {

    private static OffsetDateTime at(int day, int hour) {
        return OffsetDateTime.of(2026, 3, day, hour, 0, 0, 0, ZoneOffset.UTC);
    }

    @Test
    void statementSumsAreByTypeAndBalanceIsTheActingAccountsCurrentBalance() throws Exception { // BR-52
        String id = seedAccount().getId();
        ledger(TransactionTypeEnum.DEPOSIT, new BigDecimal("100.00"), id, at(11, 12));
        ledger(TransactionTypeEnum.PIX, new BigDecimal("30.00"), id, at(11, 13));
        ledger(TransactionTypeEnum.WITHDRAW, new BigDecimal("20.00"), id, at(11, 14));
        ledger(TransactionTypeEnum.PHONE_RECHARGE, new BigDecimal("5.00"), id, at(11, 15));

        JsonNode body = bodyOf(get("?initial-date=10/03/2026&final-date=11/03/2026").andExpect(status().isOk()));

        assertThat(body.get("transaction")).hasSize(4);
        assertThat(body.get("sumIncoming").decimalValue()).isEqualByComparingTo("100");
        assertThat(body.get("sumOutgoing").decimalValue()).isEqualByComparingTo("55");
        assertThat(body.get("balance").decimalValue()).isEqualByComparingTo("5000");
        JsonNode first = body.get("transaction").get(0);
        assertThat(first.fieldNames()).toIterable().containsExactlyInAnyOrder("transactionType", "amount", "dateHour"); // BR-53: no id
    }

    @Test
    void initialDayIsExcludedBecauseBothBoundsAreEndOfDayUtc() throws Exception { // characterizes F-07 / BR-50; BE-05 fixes
        String id = seedAccount().getId();
        ledger(TransactionTypeEnum.DEPOSIT, new BigDecimal("1.00"), id, at(10, 12));
        ledger(TransactionTypeEnum.DEPOSIT, new BigDecimal("2.00"), id, at(11, 12));

        JsonNode body = bodyOf(get("?initial-date=10/03/2026&final-date=11/03/2026").andExpect(status().isOk()));

        assertThat(body.get("transaction")).hasSize(1); // only the 11th; the 10th is lost
        assertThat(body.get("sumIncoming").decimalValue()).isEqualByComparingTo("2");
    }

    @Test
    void aSingleDayRangeReturnsNothing() throws Exception { // characterizes F-07
        ledger(TransactionTypeEnum.DEPOSIT, new BigDecimal("1.00"), seedAccount().getId(), at(11, 12));
        JsonNode body = bodyOf(get("?initial-date=11/03/2026&final-date=11/03/2026").andExpect(status().isOk()));
        assertThat(body.get("transaction")).isEmpty();
    }

    @Test
    void statementExcludesOtherAccountsTransactions() throws Exception { // F-04 / BR-51 fixed by BE-04
        AccountEntity other = createOtherAccount("9999", new BigDecimal("10.00"));
        ledger(TransactionTypeEnum.DEPOSIT, new BigDecimal("77.00"), other.getId(), at(11, 12));

        JsonNode body = bodyOf(get("?initial-date=10/03/2026&final-date=11/03/2026").andExpect(status().isOk()));

        assertThat(body.get("transaction")).isEmpty();
        assertThat(body.get("sumIncoming").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    void missingDatesAreNotHandled() { // characterizes F-07: NPE surfaces as a servlet exception (HTTP 500 in a running server)
        assertThatThrownBy(() -> get("")).hasRootCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void transactionByIdReturnsTypeAmountAndDate() throws Exception {
        Transaction saved = ledger(TransactionTypeEnum.PIX, new BigDecimal("12.50"), seedAccount().getId(), at(11, 12));

        get("/" + saved.getId()).andExpect(status().isOk())
              .andExpect(jsonPath("$.transactionType").value("PIX"))
              .andExpect(jsonPath("$.amount").value(12.50));
    }

    @Test
    void transactionByIdOfAnotherAccountIsNotFound() throws Exception { // F-04 fixed by BE-04
        AccountEntity other = createOtherAccount("9999", new BigDecimal("10.00"));
        Transaction foreign = ledger(TransactionTypeEnum.DEPOSIT, new BigDecimal("9.00"), other.getId(), at(11, 12));
        get("/" + foreign.getId()).andExpect(status().isNotFound())
              .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    @Test
    void unknownTransactionReturns404() throws Exception {
        get("/" + UUID.randomUUID()).andExpect(status().isNotFound())
              .andExpect(jsonPath("$.code").value("NOT_FOUND"))
              .andExpect(jsonPath("$.message").value("Transaction not found"));
    }

    @Test
    void nonUuidIdIsRejectedAsABadRequest() throws Exception {
        get("/not-a-uuid").andExpect(status().isBadRequest());
    }
}
