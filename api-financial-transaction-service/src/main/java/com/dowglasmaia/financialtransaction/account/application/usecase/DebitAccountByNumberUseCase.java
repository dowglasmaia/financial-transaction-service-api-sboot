package com.dowglasmaia.financialtransaction.account.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Debits the account with the given number: unknown number is 404 "Account not found.", a low balance is 422. */
@Slf4j
@Service
public class DebitAccountByNumberUseCase {

    private final AccountRepository accounts;
    private final DebitAccountUseCase debitAccount;

    public DebitAccountByNumberUseCase(AccountRepository accounts, DebitAccountUseCase debitAccount) {
        this.accounts = accounts;
        this.debitAccount = debitAccount;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void execute(String accountNumber, BigDecimal amount) {
        log.info("Start debit at account:{}", accountNumber);

        Account account = accounts.findByNumber(accountNumber)
              .orElseThrow(() -> new ObjectNotFoundExeption("Account not found."));

        debitAccount.execute(account.getId(), amount);
    }
}
