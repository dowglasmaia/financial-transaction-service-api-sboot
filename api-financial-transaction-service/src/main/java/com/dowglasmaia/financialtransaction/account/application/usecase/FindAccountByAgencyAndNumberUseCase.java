package com.dowglasmaia.financialtransaction.account.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class FindAccountByAgencyAndNumberUseCase {

    private final AccountRepository accounts;

    public FindAccountByAgencyAndNumberUseCase(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public Optional<Account> execute(String agencyId, String number) {
        return accounts.findByAgencyAndNumber(agencyId, number);
    }
}
