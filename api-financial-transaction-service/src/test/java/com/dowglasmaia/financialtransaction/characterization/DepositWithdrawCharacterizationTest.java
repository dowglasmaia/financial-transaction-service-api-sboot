package com.dowglasmaia.financialtransaction.characterization;

import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.entity.Transaction;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Characterizes CURRENT behaviour of deposit and ATM withdrawal (spec BE-01).
 * Tests tagged "characterizes F-xx" pin a known defect: a later spec must flip them deliberately.
 */
class DepositWithdrawCharacterizationTest extends AbstractIntegrationTest {

    // ---------------- deposit ----------------

    @Test
    void depositCreditsAccountAndWritesLedger() throws Exception {
        post("/deposit", "{\"agency\":\"0211\",\"account\":\"2022\",\"amount\":200.00}")
              .andExpect(status().isNoContent());

        assertThat(sameValue(new BigDecimal("5200.00"), balanceOf(SEED_ACCOUNT))).isTrue();
        List<Transaction> rows = transactionRepository.findAll();
        assertThat(rows).hasSize(1);
        Transaction row = rows.get(0);
        assertThat(row.getTransactionType()).isEqualTo(TransactionTypeEnum.DEPOSIT);
        assertThat(row.getAccountId()).isEqualTo(seedAccount().getId());
        assertThat(row.getAgencyId()).isEqualTo(seedAgency().getId());
        assertThat(sameValue(new BigDecimal("200.00"), row.getAmount())).isTrue();
        assertThat(row.getDateHour()).isNotNull();
    }

    @Test
    void depositIgnoresTheAgencyTypedByTheClient() throws Exception { // BR-21
        post("/deposit", "{\"agency\":\"9999\",\"account\":\"2022\",\"amount\":10.00}")
              .andExpect(status().isNoContent());
        assertThat(sameValue(new BigDecimal("5010.00"), balanceOf(SEED_ACCOUNT))).isTrue();
    }

    @Test
    void depositToAnotherUsersAccountIsAllowed() throws Exception { // BR-24 (third-party deposit)
        createOtherAccount("9999", new BigDecimal("100.00"));
        post("/deposit", "{\"agency\":\"0211\",\"account\":\"9999\",\"amount\":50.00}")
              .andExpect(status().isNoContent());
        assertThat(sameValue(new BigDecimal("150.00"), balanceOf("9999"))).isTrue();
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
    }

    @Test
    void depositToUnknownAccountReturns404WithCodeAndMessage() throws Exception {
        post("/deposit", "{\"agency\":\"0211\",\"account\":\"0000\",\"amount\":50.00}")
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.code").value("NOT_FOUND"))
              .andExpect(jsonPath("$.message").value("Account not found."));
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void depositValidationMessages() throws Exception { // BR-02, BR-03, BR-20
        post("/deposit", "{}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"))
              .andExpect(jsonPath("$.message").value("Account is required"));
        post("/deposit", "{\"agency\":\"0211\",\"account\":\"2022\",\"amount\":0}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Amount must be positive and non-null"));
        post("/deposit", "{\"account\":\"2022\",\"amount\":10}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Agency is required"));
    }

    // ---------------- ATM withdrawal ----------------

    @Test
    void cardBasedWithdrawalDebitsTheActingAccountAndWritesLedger() throws Exception {
        post("/atm-withdrawal", "{\"amount\":100.00}").andExpect(status().isNoContent());

        assertThat(sameValue(new BigDecimal("4900.00"), balanceOf(SEED_ACCOUNT))).isTrue();
        Transaction row = transactionRepository.findAll().get(0);
        assertThat(row.getTransactionType()).isEqualTo(TransactionTypeEnum.WITHDRAW);
        assertThat(row.getAccountId()).isEqualTo(seedAccount().getId());
        assertThat(row.getAgencyId()).isEqualTo(seedAgency().getId());
    }

    @Test
    void withdrawalOfTheExactBalanceIsRejected() throws Exception { // characterizes F-10 / BR-05 (strict '>'); BE-03 flips to '>='
        post("/atm-withdrawal", "{\"amount\":5000.00}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value(INSUFFICIENT));
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void withdrawalAboveBalanceIsRejected() throws Exception {
        post("/atm-withdrawal", "{\"amount\":5000.01}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value(INSUFFICIENT));
    }

    @Test
    void nonCardWithdrawalDebitsAnyAccountByNumberButWritesLedgerOnTheCaller() throws Exception { // characterizes F-05; BE-04 fixes
        Account other = createOtherAccount("9999", new BigDecimal("1000.00"));
        post("/atm-withdrawal", "{\"amount\":100.00,\"agency\":\"0211\",\"numberAccount\":\"9999\"}")
              .andExpect(status().isNoContent());

        assertThat(sameValue(new BigDecimal("900.00"), balanceOf("9999"))).isTrue();
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
        Transaction row = transactionRepository.findAll().get(0);
        assertThat(row.getAccountId()).isEqualTo(seedAccount().getId()); // the caller, not the debited account
        assertThat(row.getAccountId()).isNotEqualTo(other.getId());
    }

    @Test
    void nonCardWithdrawalOnUnknownAccountReturns404() throws Exception {
        post("/atm-withdrawal", "{\"amount\":10.00,\"agency\":\"0211\",\"numberAccount\":\"0000\"}")
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.message").value("Account not found."));
    }

    @Test
    void withdrawalAmountValidation() throws Exception { // BR-03, BR-30
        post("/atm-withdrawal", "{}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Amount must be positive and non-null"));
        post("/atm-withdrawal", "{\"amount\":-5}")
              .andExpect(status().isUnprocessableEntity())
              .andExpect(jsonPath("$.message").value("Amount must be positive and non-null"));
    }
}
