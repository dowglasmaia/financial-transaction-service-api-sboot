package com.dowglasmaia.financialtransaction.account.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import com.dowglasmaia.financialtransaction.shared.error.UnprocessableEntityExeption;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** The single balance rule: debit only when balance >= amount, as ONE atomic statement (see AccountRepository#tryDebit). */
@Slf4j
@Service
public class DebitAccountUseCase {

    static final String INSUFFICIENT_BALANCE = "Operation not performed, there is not enough balance in the account.";

    private final AccountRepository accounts;

    public DebitAccountUseCase(AccountRepository accounts) {
        this.accounts = accounts;
    }

    /** Balance too low: 422 with the known message. Must join the caller's transaction (REQUIRED). */
    @Transactional(propagation = Propagation.REQUIRED)
    public void execute(String accountId, BigDecimal amount) {
        if (!accounts.tryDebit(accountId, amount)) {
            log.error("Operation not performed, there is not enough balance in the account.");
            throw new UnprocessableEntityExeption(INSUFFICIENT_BALANCE);
        }
    }
}
