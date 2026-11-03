package com.dowglasmaia.financialtransaction.security;

import com.dowglasmaia.financialtransaction.account.infra.persistence.AccountEntity;
import com.dowglasmaia.financialtransaction.entity.Transaction;
import com.dowglasmaia.financialtransaction.entity.TransactionTypeEnum;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import com.dowglasmaia.financialtransaction.support.MockIssuer;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec BE-04: two real customers. A is the seeded owner (account 2022), B is linked to account 9999. Each one sees and
 * moves only their own money; a foreign resource is 404 (never 403, so its existence is not revealed).
 */
class OwnershipTest extends AbstractIntegrationTest {

    private static final String RANGE = "?initial-date=01/01/2000&final-date=31/12/2100";

    private final String tokenA = MockIssuer.ownerToken();
    private final String tokenB = MockIssuer.tokenFor(MockIssuer.OTHER_SUB);
    private AccountEntity accountB;

    @BeforeEach
    void customerB() {
        accountB = createOtherAccount("9999", new BigDecimal("1000.00"), MockIssuer.OTHER_SUB);
    }

    private Transaction row(TransactionTypeEnum type, String amount, AccountEntity account) {
        return ledger(type, new BigDecimal(amount), account.getId(), OffsetDateTime.now().minusHours(1));
    }

    // ---- reading

    @Test
    void aCustomerCannotReadAnotherCustomersTransactionById() throws Exception {
        Transaction ofB = row(TransactionTypeEnum.DEPOSIT, "9.00", accountB);

        getAs(tokenA, "/" + ofB.getId()).andExpect(status().isNotFound())
              .andExpect(jsonPath("$.code").value("NOT_FOUND"))
              .andExpect(jsonPath("$.message").value("Transaction not found"));
        getAs(tokenB, "/" + ofB.getId()).andExpect(status().isOk());
    }

    @Test
    void theStatementAndItsTotalsContainOnlyTheCallersRows() throws Exception {
        row(TransactionTypeEnum.DEPOSIT, "100.00", seedAccount());
        row(TransactionTypeEnum.DEPOSIT, "77.00", accountB);
        row(TransactionTypeEnum.WITHDRAW, "5.00", accountB);

        JsonNode a = bodyOf(getAs(tokenA, RANGE).andExpect(status().isOk()));
        assertThat(a.get("transaction")).hasSize(1);
        assertThat(a.get("sumIncoming").decimalValue()).isEqualByComparingTo("100");
        assertThat(a.get("sumOutgoing").decimalValue()).isEqualByComparingTo("0");
        assertThat(a.get("balance").decimalValue()).isEqualByComparingTo("5000");

        JsonNode b = bodyOf(getAs(tokenB, RANGE).andExpect(status().isOk()));
        assertThat(b.get("transaction")).hasSize(2);
        assertThat(b.get("sumIncoming").decimalValue()).isEqualByComparingTo("77");
        assertThat(b.get("sumOutgoing").decimalValue()).isEqualByComparingTo("5");
        assertThat(b.get("balance").decimalValue()).isEqualByComparingTo("1000");
    }

    // ---- writing

    @Test
    void aCustomerCannotDebitAnotherCustomersAccountByNumber() throws Exception {
        postAs(tokenA, "/atm-withdrawal", "{\"amount\":100.00,\"agency\":\"0211\",\"numberAccount\":\"9999\"}")
              .andExpect(status().isNotFound())
              .andExpect(jsonPath("$.message").value("Account not found."));

        assertThat(sameValue(new BigDecimal("1000.00"), balanceOf("9999"))).as("B untouched").isTrue();
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).as("A untouched").isTrue();
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void aNonCardWithdrawalOnTheCallersOwnAccountDebitsItAndTheLedgerFollowsIt() throws Exception {
        postAs(tokenA, "/atm-withdrawal", "{\"amount\":100.00,\"agency\":\"0211\",\"numberAccount\":\"2022\"}")
              .andExpect(status().isNoContent());

        assertThat(sameValue(new BigDecimal("4900.00"), balanceOf(SEED_ACCOUNT))).isTrue();
        assertThat(transactionRepository.findAll()).singleElement()
              .satisfies(t -> assertThat(t.getAccountId()).isEqualTo(seedAccount().getId()));
    }

    @Test
    void eachCustomerOperatesOnlyOnTheirOwnAccount() throws Exception {
        postAs(tokenB, "/atm-withdrawal", "{\"amount\":100.00}").andExpect(status().isNoContent());
        postAs(tokenB, "/recharge-mobile", "{\"phoneNumber\":998877665,\"areaCode\":11,\"carrier\":\"VIVO\",\"amount\":50.00}")
              .andExpect(status().isNoContent());

        assertThat(sameValue(new BigDecimal("850.00"), balanceOf("9999"))).as("B debited").isTrue();
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).as("A untouched").isTrue();
        assertThat(transactionRepository.findAll()).hasSize(2)
              .allSatisfy(t -> assertThat(t.getAccountId()).isEqualTo(accountB.getId()));
    }

    @Test
    void aThirdPartyDepositIsStillAllowedAndLandsInTheReceiversStatementOnly() throws Exception {
        postAs(tokenA, "/deposit", "{\"agency\":\"0211\",\"account\":\"9999\",\"amount\":50.00}").andExpect(status().isNoContent());

        assertThat(sameValue(new BigDecimal("1050.00"), balanceOf("9999"))).isTrue();
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
        assertThat(bodyOf(getAs(tokenB, RANGE)).get("transaction")).hasSize(1);
        assertThat(bodyOf(getAs(tokenA, RANGE)).get("transaction")).isEmpty();
    }
}
