package com.dowglasmaia.financialtransaction.account.application;

import com.dowglasmaia.financialtransaction.account.domain.Account;
import com.dowglasmaia.financialtransaction.account.domain.AccountRepository;
import com.dowglasmaia.financialtransaction.account.domain.AgencyRepository;
import com.dowglasmaia.financialtransaction.account.domain.CallerIdentity;
import com.dowglasmaia.financialtransaction.account.domain.User;
import com.dowglasmaia.financialtransaction.account.domain.UserRepository;
import com.dowglasmaia.financialtransaction.exeptions.ForbiddenExeption;
import com.dowglasmaia.financialtransaction.exeptions.ObjectNotFoundExeption;
import com.dowglasmaia.financialtransaction.exeptions.UnprocessableEntityExeption;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Everything about accounts that other features use: the single balance rule (debit when balance >= amount, credit),
 * the agency of the demo bank, and the caller's own account resolved from the token subject (decision D-25). There is
 * no auto-provisioning: a subject not linked to a user gets 403.
 */
@Slf4j
@Service
public class AccountService {

    static final String INSUFFICIENT_BALANCE = "Operation not performed, there is not enough balance in the account.";
    static final String NOT_LINKED = "Access denied: this identity is not linked to a customer.";
    private static final String CURRENT_AGENCY_NUMBER = "0211";

    private final AccountRepository accounts;
    private final AgencyRepository agencies;
    private final UserRepository users;
    private final CallerIdentity caller;

    public AccountService(AccountRepository accounts, AgencyRepository agencies, UserRepository users, CallerIdentity caller) {
        this.accounts = accounts;
        this.agencies = agencies;
        this.users = users;
        this.caller = caller;
    }

    /** Debits the account with the given number. Unknown: 404 "Account not found."; balance too low: 422. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void debit(String accountNumber, BigDecimal amount) {
        log.info("Start debit at account:{}", accountNumber);

        Account account = accounts.findByNumber(accountNumber)
              .orElseThrow(() -> new ObjectNotFoundExeption("Account not found."));

        debitById(account.getId(), amount);
    }

    /** One atomic statement: the balance check and the update cannot be separated by another request. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void debitById(String accountId, BigDecimal amount) {
        if (!accounts.tryDebit(accountId, amount)) {
            log.error("Operation not performed, there is not enough balance in the account.");
            throw new UnprocessableEntityExeption(INSUFFICIENT_BALANCE);
        }
    }

    /** Atomic credit; an unknown account id changes nothing (as before). */
    @Transactional(propagation = Propagation.REQUIRED)
    public void credit(String accountId, BigDecimal amount) {
        accounts.credit(accountId, amount);
    }

    public Optional<Account> findByAgencyAndNumber(String agencyId, String number) {
        return accounts.findByAgencyAndNumber(agencyId, number);
    }

    /** The id of the agency the operations run in (the demo bank has one). */
    public String currentAgencyId() {
        log.info("Start getCurrentAgency");
        return agencies.findByNumber(CURRENT_AGENCY_NUMBER)
              .orElseThrow(() -> new ObjectNotFoundExeption("Agency not found."))
              .getId();
    }

    /** The caller's account (one per user). Not linked, or linked without an account: 403. */
    public Account currentAccount() {
        User user = currentUser().orElseThrow(() -> new ForbiddenExeption(NOT_LINKED));
        return accounts.findFirstByUserId(user.getId()).orElseThrow(() -> new ForbiddenExeption(NOT_LINKED));
    }

    /** True when the token subject is linked to a customer. */
    public boolean isLinked() {
        return currentUser().isPresent();
    }

    private Optional<User> currentUser() {
        String subject = caller.subject().orElseThrow(() -> new ForbiddenExeption("Access denied."));
        return users.findByExternalSubject(subject);
    }
}
