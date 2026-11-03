package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.Statement;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    void asksForCompletedRowsOfTheCallersAccountUntilTheEndOfEachDayAndAddsTheBalance() {
        when(getCurrentAccount.execute()).thenReturn(Account.restore("acc-1", null, null, "2022", new BigDecimal("4900.00")));
        OffsetDateTime from = LocalTime.MAX.atDate(java.time.LocalDate.of(2026, 3, 1)).atOffset(ZoneOffset.UTC);
        OffsetDateTime to = LocalTime.MAX.atDate(java.time.LocalDate.of(2026, 3, 31)).atOffset(ZoneOffset.UTC);
        Transaction row = Transaction.restore("t-1", TransactionType.DEPOSIT, "acc-1", BigDecimal.TEN, "agency-1", TransactionStatus.COMPLETED, from);
        when(ledger.findByStatusAndAccountIdAndDateHourBetween(TransactionStatus.COMPLETED, "acc-1", from, to)).thenReturn(List.of(row));

        Statement statement = new GetStatementUseCase(ledger, getCurrentAccount).execute("01/03/2026", "31/03/2026");

        assertThat(statement.transactions()).containsExactly(row);
        assertThat(statement.balance()).isEqualByComparingTo("4900.00");
    }
}
