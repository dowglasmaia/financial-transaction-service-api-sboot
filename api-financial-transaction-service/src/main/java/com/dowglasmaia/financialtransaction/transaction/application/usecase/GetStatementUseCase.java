package com.dowglasmaia.financialtransaction.transaction.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.usecase.GetCurrentAccountUseCase;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.transaction.application.port.TransactionRepository;
import com.dowglasmaia.financialtransaction.transaction.domain.DateConverter;
import com.dowglasmaia.financialtransaction.transaction.domain.Statement;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** The completed entries of the caller's account between two days (dd/MM/yyyy, end of day UTC) and the current balance. */
@Slf4j
@Service
public class GetStatementUseCase {

    private final TransactionRepository ledger;
    private final GetCurrentAccountUseCase getCurrentAccount;

    public GetStatementUseCase(TransactionRepository ledger, GetCurrentAccountUseCase getCurrentAccount) {
        this.ledger = ledger;
        this.getCurrentAccount = getCurrentAccount;
    }

    public Statement execute(String initialDate, String finalDate) {
        log.info("Start findByDateRange Transactions");

        Account account = getCurrentAccount.execute();

        var startDate = DateConverter.convertStringToOffsetDateTime(initialDate);
        log.info("startDate: {}", startDate);

        var endDate = DateConverter.convertStringToOffsetDateTime(finalDate);
        log.info("endDate: {}", endDate);

        var rows = ledger.findByStatusAndAccountIdAndDateHourBetween(TransactionStatus.COMPLETED, account.getId(), startDate, endDate);

        return new Statement(rows, account.getBalance());
    }
}
