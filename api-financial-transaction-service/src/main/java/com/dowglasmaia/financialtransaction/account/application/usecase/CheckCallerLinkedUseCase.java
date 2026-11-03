package com.dowglasmaia.financialtransaction.account.application.usecase;

import com.dowglasmaia.financialtransaction.account.application.port.CallerIdentity;
import com.dowglasmaia.financialtransaction.account.application.port.UserRepository;
import com.dowglasmaia.financialtransaction.shared.error.ForbiddenException;
import org.springframework.stereotype.Service;

/** True when the token subject is linked to a customer. */
@Service
public class CheckCallerLinkedUseCase {

    private final UserRepository users;
    private final CallerIdentity caller;

    public CheckCallerLinkedUseCase(UserRepository users, CallerIdentity caller) {
        this.users = users;
        this.caller = caller;
    }

    public boolean execute() {
        String subject = caller.subject().orElseThrow(() -> new ForbiddenException("Access denied."));
        return users.findByExternalSubject(subject).isPresent();
    }
}
