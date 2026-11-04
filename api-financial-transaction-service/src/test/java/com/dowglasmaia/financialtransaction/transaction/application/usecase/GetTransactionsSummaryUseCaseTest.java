package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.DailyTotal;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementPeriod;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementTotals;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionsSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetTransactionsSummaryUseCaseTest {

    @Mock TransactionRepository ledger;
    @Mock GetCurrentAccountUseCase getCurrentAccount;

    @Test
    void totalsAndDailyBucketsOfTheCallersAccountForThePeriod() {
        when(getCurrentAccount.execute()).thenReturn(Account.restore("acc-1", null, null, "2022", new BigDecimal("4900.00")));
        StatementPeriod period = StatementPeriod.ofDays(LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11));
        when(ledger.totals("acc-1", period, null)).thenReturn(new StatementTotals(new BigDecimal("100.00"), new BigDecimal("35.00"), 3));
        when(ledger.dailyTotals("acc-1", period)).thenReturn(List.of(new DailyTotal(LocalDate.of(2026, 3, 11), new BigDecimal("100.00"), new BigDecimal("35.00"))));

        TransactionsSummary summary = new GetTransactionsSummaryUseCase(ledger, getCurrentAccount).execute(period);

        assertThat(summary.sumIncoming()).isEqualByComparingTo("100.00");
        assertThat(summary.sumOutgoing()).isEqualByComparingTo("35.00");
        assertThat(summary.balance()).isEqualByComparingTo("4900.00");
        assertThat(summary.series()).hasSize(2);
    }
}
