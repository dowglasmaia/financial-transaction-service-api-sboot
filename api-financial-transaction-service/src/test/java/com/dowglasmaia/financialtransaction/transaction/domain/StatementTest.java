package com.dowglasmaia.financialtransaction.transaction.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatementTest {

    @Test
    void carriesTheEntriesAndTheCurrentBalance() {
        Transaction entry = Transaction.create(TransactionType.DEPOSIT, "acc-1", BigDecimal.TEN, "agency-1", TransactionStatus.COMPLETED, null);

        Statement statement = new Statement(List.of(entry), new BigDecimal("5000.00"));

        assertThat(statement.transactions()).containsExactly(entry);
        assertThat(statement.balance()).isEqualByComparingTo("5000.00");
    }
}
