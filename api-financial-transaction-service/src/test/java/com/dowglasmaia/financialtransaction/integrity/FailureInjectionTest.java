package com.dowglasmaia.financialtransaction.integrity;

import com.dowglasmaia.financialtransaction.entity.OutboxEvent;
import com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence.TransactionEntity;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.repository.OutboxEventRepository;
import com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence.TransactionJpaRepository;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Spec BE-03: when the database fails half way through an operation, balance and ledger stay consistent
 * (everything or nothing), and an event is never published for an operation that did not commit.
 */
class FailureInjectionTest extends AbstractIntegrationTest {

    @SpyBean TransactionJpaRepository ledger;
    @SpyBean OutboxEventRepository outbox;

    private void ledgerFails() {
        doThrow(new IllegalStateException("ledger unavailable")).when(ledger).save(any(TransactionEntity.class));
    }

    @Test
    void aLedgerFailureRollsBackADeposit() {
        ledgerFails();
        assertThatThrownBy(() -> post("/deposit", "{\"agency\":\"0211\",\"account\":\"2022\",\"amount\":200.00}"))
              .hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).as("credit rolled back").isTrue();
    }

    @Test
    void aLedgerFailureRollsBackAWithdrawal() {
        ledgerFails();
        assertThatThrownBy(() -> post("/atm-withdrawal", "{\"amount\":100.00}"))
              .hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).as("debit rolled back").isTrue();
    }

    @Test
    void aLedgerFailureRollsBackARechargeAndPublishesNothing() {
        ledgerFails();
        assertThatThrownBy(() -> post("/recharge-mobile", "{\"phoneNumber\":998877665,\"areaCode\":11,\"carrier\":\"VIVO\",\"amount\":50.00}"))
              .hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).isTrue();
        assertThat(outboxEventRepository.count()).as("no event for an operation that did not commit").isZero();
    }

    @Test
    void anOutboxFailureRollsBackTheWholeRecharge() {
        doThrow(new IllegalStateException("outbox unavailable")).when(outbox).save(any(OutboxEvent.class));
        assertThatThrownBy(() -> post("/recharge-mobile", "{\"phoneNumber\":998877665,\"areaCode\":11,\"carrier\":\"VIVO\",\"amount\":50.00}"))
              .hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).as("debit rolled back").isTrue();
        assertThat(transactionRepository.count()).as("ledger row rolled back").isZero();
    }

    @Test
    void aLedgerFailureWhileReservingAPixLeavesTheBalanceUntouchedAndNeverCallsMaiaBank() {
        ledgerFails();
        assertThatThrownBy(() -> post("/send-pix", "{\"pixKey\":\"82215344040\",\"keyType\":\"cpf\",\"transactionAmount\":500.00}"))
              .hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).as("reservation rolled back").isTrue();
        verify(maiaBankClient, never()).receiversPix();
    }

    @Test
    void aMaiaBankFailureCompensatesTheReservationAtomically() throws Exception {
        org.mockito.Mockito.when(maiaBankClient.receiversPix()).thenThrow(new RuntimeException("MaiaBank down"));

        post("/send-pix", "{\"pixKey\":\"82215344040\",\"keyType\":\"cpf\",\"transactionAmount\":500.00}")
              .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnprocessableEntity());

        assertThat(sameValue(SEED_BALANCE, balanceOf(SEED_ACCOUNT))).as("debit reversed").isTrue();
        List<TransactionEntity> rows = transactionRepository.findAll();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getTransactionType()).isEqualTo(TransactionType.PIX);
        assertThat(rows.get(0).getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(sum(rows)).isEqualByComparingTo("500.00"); // the audit row keeps the amount, the statement ignores it
        assertThat(bodyOf(get("?initial-date=01/01/2000&final-date=31/12/2100")).get("transaction")).isEmpty();
    }

    private static BigDecimal sum(List<TransactionEntity> rows) {
        return rows.stream().map(TransactionEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
