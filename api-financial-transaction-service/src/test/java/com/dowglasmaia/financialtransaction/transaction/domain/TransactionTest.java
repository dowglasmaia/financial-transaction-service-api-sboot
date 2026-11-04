package com.dowglasmaia.financialtransaction.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class TransactionTest {

    private static final OffsetDateTime WHEN = OffsetDateTime.parse("2026-01-01T10:00:00Z");

    @Test
    void aNewLedgerEntryHasNoIdUntilItIsStored() {
        Transaction entry = Transaction.create(TransactionType.PIX, "acc-1", new BigDecimal("12.50"), "agency-1", TransactionStatus.PENDING, WHEN);

        assertThat(entry.getId()).isNull();
        assertThat(entry.getTransactionType()).isEqualTo(TransactionType.PIX);
        assertThat(entry.getAccountId()).isEqualTo("acc-1");
        assertThat(entry.getAgencyId()).isEqualTo("agency-1");
        assertThat(entry.getAmount()).isEqualByComparingTo("12.50");
        assertThat(entry.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(entry.getDateHour()).isEqualTo(WHEN);
    }

    @Test
    void restoreRebuildsStoredState() {
        Transaction entry = Transaction.restore("t-1", TransactionType.DEPOSIT, "acc-1", BigDecimal.TEN, null, TransactionStatus.COMPLETED, WHEN);

        assertThat(entry.getId()).isEqualTo("t-1");
        assertThat(entry.getAgencyId()).isNull();
        assertThat(entry.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }
}
