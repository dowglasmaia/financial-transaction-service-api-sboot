package com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Round trips of the ledger adapter against the real PostgreSQL schema (port in, domain objects out). */
class TransactionRepositoryAdapterTest extends AbstractIntegrationTest {

    private static final OffsetDateTime NOON = OffsetDateTime.parse("2026-03-10T12:00:00Z");

    @Autowired TransactionRepository ledger;

    private Transaction entry(TransactionStatus status) {
        return Transaction.create(TransactionType.PIX, seedAccount().getId(), new BigDecimal("12.50"), seedAgency().getId(), status, NOON);
    }

    @Test
    void savesAnEntryAndReadsItBackWithAnId() {
        Transaction saved = ledger.save(entry(TransactionStatus.PENDING));

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(saved.getAmount()).isEqualByComparingTo("12.50");
        assertThat(saved.getTransactionType()).isEqualTo(TransactionType.PIX);
    }

    @Test
    void anEntryIsFoundOnlyThroughItsOwnAccount() {
        Transaction saved = ledger.save(entry(TransactionStatus.COMPLETED));

        assertThat(ledger.findByIdAndAccountId(saved.getId(), seedAccount().getId())).isPresent();
        assertThat(ledger.findByIdAndAccountId(saved.getId(), "another-account")).isEmpty();
    }

    @Test
    void theStatementQueryFiltersStatusAccountAndPeriod() {
        ledger.save(entry(TransactionStatus.COMPLETED));
        ledger.save(entry(TransactionStatus.PENDING));
        ledger.save(entry(TransactionStatus.FAILED));

        List<Transaction> rows = ledger.findByStatusAndAccountIdAndDateHourBetween(TransactionStatus.COMPLETED, seedAccount().getId(),
              NOON.minusDays(1), NOON.plusDays(1));
        List<Transaction> outside = ledger.findByStatusAndAccountIdAndDateHourBetween(TransactionStatus.COMPLETED, seedAccount().getId(),
              NOON.plusDays(1), NOON.plusDays(2));

        assertThat(rows).hasSize(1);
        assertThat(outside).isEmpty();
    }

    @Test
    void updatesTheStatusWithASingleStatement() {
        Transaction saved = ledger.save(entry(TransactionStatus.PENDING));

        ledger.updateStatus(saved.getId(), TransactionStatus.COMPLETED);

        assertThat(ledger.findByIdAndAccountId(saved.getId(), seedAccount().getId())).get()
              .extracting(Transaction::getStatus).isEqualTo(TransactionStatus.COMPLETED);
    }
}
