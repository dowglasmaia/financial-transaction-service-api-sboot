package com.dowglasmaia.financialtransaction.transaction.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A ledger entry of one account. A snapshot: it changes status only through the repository's single-statement
 * {@code updateStatus} (a Pix reservation goes PENDING -> COMPLETED or FAILED), so it has no mutators.
 */
public final class Transaction {

    private final String id;
    private final TransactionType transactionType;
    private final String accountId;
    private final BigDecimal amount;
    private final String agencyId;
    private final TransactionStatus status;
    private final OffsetDateTime dateHour;

    private Transaction(String id, TransactionType transactionType, String accountId, BigDecimal amount, String agencyId,
                        TransactionStatus status, OffsetDateTime dateHour) {
        this.id = id;
        this.transactionType = transactionType;
        this.accountId = accountId;
        this.amount = amount;
        this.agencyId = agencyId;
        this.status = status;
        this.dateHour = dateHour;
    }

    public static Transaction create(TransactionType transactionType, String accountId, BigDecimal amount, String agencyId,
                                     TransactionStatus status, OffsetDateTime dateHour) {
        return new Transaction(null, transactionType, accountId, amount, agencyId, status, dateHour);
    }

    public static Transaction restore(String id, TransactionType transactionType, String accountId, BigDecimal amount,
                                      String agencyId, TransactionStatus status, OffsetDateTime dateHour) {
        return new Transaction(id, transactionType, accountId, amount, agencyId, status, dateHour);
    }

    public String getId() {
        return id;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public OffsetDateTime getDateHour() {
        return dateHour;
    }
}
