package com.dowglasmaia.financialtransaction.services.client.account;

import java.math.BigDecimal;

/**
 * Single place of the balance rule. Despite the historical name of this interface (kept: no class renames, D-19),
 * both operations DEBIT an account, atomically, allowing the debit when {@code balance >= amount}.
 */
public interface AddAmountService {

    /**
     * Debits the account with the given number. Not found: 404 "Account not found."; balance lower than the amount:
     * 422 "Operation not performed, there is not enough balance in the account.". Must run inside a transaction.
     */
    void debit(String accountNumber, BigDecimal amount);

    /** Same as {@link #debit(String, BigDecimal)} for an account id. */
    void debitById(String accountId, BigDecimal amount);
}
