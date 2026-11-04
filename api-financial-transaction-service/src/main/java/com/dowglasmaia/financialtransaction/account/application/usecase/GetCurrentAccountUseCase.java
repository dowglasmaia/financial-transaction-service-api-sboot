package com.dowglasmaia.financialtransaction.account.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.port.AccountRepository;
import com.dowglasmaia.financialtransaction.account.application.port.CallerIdentity;
import com.dowglasmaia.financialtransaction.account.application.port.UserRepository;
import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.account.domain.User;
import com.dowglasmaia.financialtransaction.shared.error.ForbiddenException;
import org.springframework.stereotype.Service;

/**
 * The caller's account (one per user, decision D-25), resolved from the token subject. There is no auto-provisioning:
 * a subject not linked to a user, or a user without an account, gets 403.
 */
@Service
public class GetCurrentAccountUseCase {

    static final String NOT_LINKED = "Access denied: this identity is not linked to a customer.";

    private final AccountRepository accounts;
    private final UserRepository users;
    private final CallerIdentity caller;

    public GetCurrentAccountUseCase(AccountRepository accounts, UserRepository users, CallerIdentity caller) {
        this.accounts = accounts;
        this.users = users;
        this.caller = caller;
    }

    public Account execute() {
        String subject = caller.subject().orElseThrow(() -> new ForbiddenException("Access denied."));
        User user = users.findByExternalSubject(subject).orElseThrow(() -> new ForbiddenException(NOT_LINKED));
        return accounts.findFirstByUserId(user.getId()).orElseThrow(() -> new ForbiddenException(NOT_LINKED));
    }
}
