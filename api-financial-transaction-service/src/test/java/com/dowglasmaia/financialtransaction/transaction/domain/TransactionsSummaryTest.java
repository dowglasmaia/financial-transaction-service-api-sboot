package com.dowglasmaia.financialtransaction.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionsSummaryTest {

    @Test
    void hasOneBucketPerDayOfThePeriodEvenWhenADayHadNoActivity() {
        StatementPeriod period = StatementPeriod.ofDays(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));
        StatementTotals totals = new StatementTotals(new BigDecimal("100.00"), new BigDecimal("35.00"), 3);
        List<DailyTotal> found = List.of(
              new DailyTotal(LocalDate.of(2026, 3, 12), new BigDecimal("100.00"), BigDecimal.ZERO),
              new DailyTotal(LocalDate.of(2026, 3, 10), BigDecimal.ZERO, new BigDecimal("35.00")));

        TransactionsSummary summary = TransactionsSummary.of(period, totals, found, new BigDecimal("4900.00"));

        assertThat(summary.series()).extracting(DailyTotal::date)
              .containsExactly(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11), LocalDate.of(2026, 3, 12));
        assertThat(summary.series().get(1).incoming()).isEqualByComparingTo("0");
        assertThat(summary.series().get(1).outgoing()).isEqualByComparingTo("0");
        assertThat(summary.series().get(0).outgoing()).isEqualByComparingTo("35.00");
        assertThat(summary.series().get(2).incoming()).isEqualByComparingTo("100.00");
        assertThat(summary.sumIncoming()).isEqualByComparingTo("100.00");
        assertThat(summary.sumOutgoing()).isEqualByComparingTo("35.00");
        assertThat(summary.balance()).isEqualByComparingTo("4900.00");
    }
}
