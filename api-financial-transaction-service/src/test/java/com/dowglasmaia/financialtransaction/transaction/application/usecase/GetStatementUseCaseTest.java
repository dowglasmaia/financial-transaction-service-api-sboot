package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.Statement;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementPeriod;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementQuery;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementTotals;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetStatementUseCaseTest {

    @Mock TransactionRepository ledger;
    @Mock GetCurrentAccountUseCase getCurrentAccount;

    @Test
    void asksTheLedgerForTheRowsAndTheTotalsOfTheCallersAccountAndAddsTheBalance() {
        when(getCurrentAccount.execute()).thenReturn(Account.restore("acc-1", null, null, "2022", new BigDecimal("4900.00")));
        StatementPeriod period = StatementPeriod.ofDays(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        StatementQuery query = StatementQuery.of(period, "DEPOSIT", null, null, 1, 10);
        Transaction row = Transaction.restore("t-1", TransactionType.DEPOSIT, "acc-1", BigDecimal.TEN, "agency-1", TransactionStatus.COMPLETED, period.start());
        when(ledger.findStatementRows("acc-1", query)).thenReturn(List.of(row));
        when(ledger.totals("acc-1", period, TransactionType.DEPOSIT)).thenReturn(new StatementTotals(new BigDecimal("10.00"), BigDecimal.ZERO, 11));

        Statement statement = new GetStatementUseCase(ledger, getCurrentAccount).execute(query);

        assertThat(statement.transactions()).containsExactly(row);
        assertThat(statement.balance()).isEqualByComparingTo("4900.00");
        assertThat(statement.sumIncoming()).isEqualByComparingTo("10.00");
        assertThat(statement.totalElements()).isEqualTo(11);
        assertThat(statement.page()).isEqualTo(1);
        assertThat(statement.size()).isEqualTo(10);
    }
}
