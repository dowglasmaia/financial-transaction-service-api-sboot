package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementPeriod;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionsSummary;
import org.springframework.stereotype.Service;

/** Home numbers: totals of a period, the daily series for the chart and the balance, always of the caller's own account. */
@Service
public class GetTransactionsSummaryUseCase {

    private final TransactionRepository ledger;
    private final GetCurrentAccountUseCase getCurrentAccount;

    public GetTransactionsSummaryUseCase(TransactionRepository ledger, GetCurrentAccountUseCase getCurrentAccount) {
        this.ledger = ledger;
        this.getCurrentAccount = getCurrentAccount;
    }

    public TransactionsSummary execute(StatementPeriod period) {
        Account account = getCurrentAccount.execute();
        var totals = ledger.totals(account.getId(), period, null);
        var days = ledger.dailyTotals(account.getId(), period);
        return TransactionsSummary.of(period, totals, days, account.getBalance());
    }
}
