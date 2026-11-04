package com.dowglasmaia.financialtransaction.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatementTest {

    @Test
    void carriesTheRowsTheTotalsOfThePeriodTheBalanceAndThePaging() {
        Transaction entry = Transaction.create(TransactionType.DEPOSIT, "acc-1", BigDecimal.TEN, "agency-1", TransactionStatus.COMPLETED, null);
        StatementTotals totals = new StatementTotals(new BigDecimal("10.00"), BigDecimal.ZERO, 42);

        Statement statement = Statement.of(List.of(entry), new BigDecimal("5000.00"), totals, 2, 20);

        assertThat(statement.transactions()).containsExactly(entry);
        assertThat(statement.balance()).isEqualByComparingTo("5000.00");
        assertThat(statement.sumIncoming()).isEqualByComparingTo("10.00");
        assertThat(statement.sumOutgoing()).isEqualByComparingTo("0");
        assertThat(statement.totalElements()).isEqualTo(42);
        assertThat(statement.page()).isEqualTo(2);
        assertThat(statement.size()).isEqualTo(20);
    }
}
