package com.dowglasmaia.financialtransaction.transaction.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.DailyTotal;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementPeriod;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementQuery;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementTotals;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import com.dowglasmaia.financialtransaction.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    private Transaction entryAt(TransactionType type, String amount, String instant, TransactionStatus status) {
        return ledger.save(Transaction.create(type, seedAccount().getId(), new BigDecimal(amount), seedAgency().getId(), status, OffsetDateTime.parse(instant)));
    }

    private StatementQuery query(StatementPeriod period, String type, String sort, String order, int page, int size) {
        return StatementQuery.of(period, type, sort, order, page, size);
    }

    @Test
    void theStatementRowsAreCompletedOnlyOfTheAccountAndInsideTheHalfOpenPeriod() {
        StatementPeriod day = StatementPeriod.ofDays(LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 11)); // 03:00Z .. 03:00Z of the 12th
        entryAt(TransactionType.DEPOSIT, "1.00", "2026-03-11T03:00:00Z", TransactionStatus.COMPLETED); // first instant: in
        entryAt(TransactionType.DEPOSIT, "2.00", "2026-03-12T02:59:59Z", TransactionStatus.COMPLETED); // last second: in
        entryAt(TransactionType.DEPOSIT, "3.00", "2026-03-11T02:59:59Z", TransactionStatus.COMPLETED); // the 10th in Sao Paulo: out
        entryAt(TransactionType.DEPOSIT, "4.00", "2026-03-12T03:00:00Z", TransactionStatus.COMPLETED); // exclusive end: out
        entryAt(TransactionType.PIX, "5.00", "2026-03-11T12:00:00Z", TransactionStatus.PENDING);
        entryAt(TransactionType.PIX, "6.00", "2026-03-11T13:00:00Z", TransactionStatus.FAILED);

        List<Transaction> rows = ledger.findStatementRows(seedAccount().getId(), query(day, null, "amount", "asc", 0, 20));

        assertThat(rows).extracting(Transaction::getAmount).usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
              .containsExactly(new BigDecimal("1.00"), new BigDecimal("2.00"));
    }

    @Test
    void sortingPagingAndTheTypeFilterWorkInTheDatabase() {
        StatementPeriod period = StatementPeriod.ofDays(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));
        entryAt(TransactionType.DEPOSIT, "10.00", "2026-03-10T12:00:00Z", TransactionStatus.COMPLETED);
        entryAt(TransactionType.PIX, "30.00", "2026-03-11T12:00:00Z", TransactionStatus.COMPLETED);
        entryAt(TransactionType.PIX, "20.00", "2026-03-12T12:00:00Z", TransactionStatus.COMPLETED);

        List<Transaction> byAmountDesc = ledger.findStatementRows(seedAccount().getId(), query(period, null, "amount", "desc", 0, 2));
        List<Transaction> secondPage = ledger.findStatementRows(seedAccount().getId(), query(period, null, "amount", "desc", 1, 2));
        List<Transaction> onlyPix = ledger.findStatementRows(seedAccount().getId(), query(period, "PIX", "date", "asc", 0, 20));

        assertThat(byAmountDesc).extracting(t -> t.getAmount().intValue()).containsExactly(30, 20);
        assertThat(secondPage).extracting(t -> t.getAmount().intValue()).containsExactly(10);
        assertThat(onlyPix).extracting(t -> t.getAmount().intValue()).containsExactly(30, 20); // date ascending
    }

    @Test
    void totalsAreClassifiedByTypeAndIgnoreThePageButNotTheTypeFilterForTheCount() {
        StatementPeriod period = StatementPeriod.ofDays(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));
        entryAt(TransactionType.DEPOSIT, "100.00", "2026-03-10T12:00:00Z", TransactionStatus.COMPLETED);
        entryAt(TransactionType.PIX, "30.00", "2026-03-11T12:00:00Z", TransactionStatus.COMPLETED);
        entryAt(TransactionType.WITHDRAW, "20.00", "2026-03-11T13:00:00Z", TransactionStatus.COMPLETED);
        entryAt(TransactionType.PHONE_RECHARGE, "5.00", "2026-03-12T12:00:00Z", TransactionStatus.COMPLETED);
        entryAt(TransactionType.PIX, "999.00", "2026-03-12T13:00:00Z", TransactionStatus.PENDING); // never counted

        StatementTotals all = ledger.totals(seedAccount().getId(), period, null);
        StatementTotals onlyPix = ledger.totals(seedAccount().getId(), period, TransactionType.PIX);

        assertThat(all.sumIncoming()).isEqualByComparingTo("100");
        assertThat(all.sumOutgoing()).isEqualByComparingTo("55");
        assertThat(all.matching()).isEqualTo(4);
        assertThat(onlyPix.sumIncoming()).isEqualByComparingTo("100"); // the sums stay those of the whole period
        assertThat(onlyPix.sumOutgoing()).isEqualByComparingTo("55");
        assertThat(onlyPix.matching()).isEqualTo(1);        // the count follows the filter
    }

    @Test
    void noRowsGiveZeroTotals() {
        StatementPeriod period = StatementPeriod.ofDays(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 2));

        StatementTotals totals = ledger.totals(seedAccount().getId(), period, null);

        assertThat(totals.sumIncoming()).isEqualByComparingTo("0");
        assertThat(totals.sumOutgoing()).isEqualByComparingTo("0");
        assertThat(totals.matching()).isZero();
    }

    @Test
    void dailyBucketsFollowTheSaoPauloDayNotTheUtcDay() {
        StatementPeriod period = StatementPeriod.ofDays(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11));
        entryAt(TransactionType.DEPOSIT, "100.00", "2026-03-11T02:30:00Z", TransactionStatus.COMPLETED); // 23:30 of the 10th in Sao Paulo
        entryAt(TransactionType.PIX, "35.00", "2026-03-11T03:30:00Z", TransactionStatus.COMPLETED);      // 00:30 of the 11th in Sao Paulo

        List<DailyTotal> days = ledger.dailyTotals(seedAccount().getId(), period);

        assertThat(days).extracting(DailyTotal::date).containsExactly(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11));
        assertThat(days.get(0).incoming()).isEqualByComparingTo("100.00");
        assertThat(days.get(0).outgoing()).isEqualByComparingTo("0");
        assertThat(days.get(1).outgoing()).isEqualByComparingTo("35.00");
    }

    @Test
    void updatesTheStatusWithASingleStatement() {
        Transaction saved = ledger.save(entry(TransactionStatus.PENDING));

        ledger.updateStatus(saved.getId(), TransactionStatus.COMPLETED);

        assertThat(ledger.findByIdAndAccountId(saved.getId(), seedAccount().getId())).get()
              .extracting(Transaction::getStatus).isEqualTo(TransactionStatus.COMPLETED);
    }
}
