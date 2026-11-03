package com.dowglasmaia.financialtransaction.account.domain;

import java.math.BigDecimal;
import java.util.Optional;

/** Output port of the account data. */
public interface AccountRepository {

    Optional<Account> findByNumber(String number);

    /** The caller's account: one account per user (decision D-25). */
    Optional<Account> findFirstByUserId(String userId);

    Optional<Account> findByAgencyAndNumber(String agencyId, String number);

    Account save(Account account);

    /**
     * The single balance rule: debit only when the balance covers the amount (balance >= amount), as ONE atomic
     * statement, so concurrent requests can never overdraw or lose an update. False = not enough balance.
     */
    boolean tryDebit(String accountId, BigDecimal amount);

    /** Atomic credit. False = unknown account. */
    boolean credit(String accountId, BigDecimal amount);
}
