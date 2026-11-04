package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.Statement;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementQuery;
import org.springframework.stereotype.Service;

/** One page of the completed entries of the caller's account for a period, with the totals of the whole period and the balance. */
@Service
public class GetStatementUseCase {

    private final TransactionRepository ledger;
    private final GetCurrentAccountUseCase getCurrentAccount;

    public GetStatementUseCase(TransactionRepository ledger, GetCurrentAccountUseCase getCurrentAccount) {
        this.ledger = ledger;
        this.getCurrentAccount = getCurrentAccount;
    }

    public Statement execute(StatementQuery query) {
        Account account = getCurrentAccount.execute();
        var rows = ledger.findStatementRows(account.getId(), query);
        var totals = ledger.totals(account.getId(), query.period(), query.type());
        return Statement.of(rows, account.getBalance(), totals, query.page(), query.size());
    }
}
