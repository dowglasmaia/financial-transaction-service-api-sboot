package com.dowglasmaia.financialtransaction.account.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Atomic credit; an unknown account id changes nothing. */
@Service
public class CreditAccountUseCase {

    private final AccountRepository accounts;

    public CreditAccountUseCase(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void execute(String accountId, BigDecimal amount) {
        accounts.credit(accountId, amount);
    }
}
