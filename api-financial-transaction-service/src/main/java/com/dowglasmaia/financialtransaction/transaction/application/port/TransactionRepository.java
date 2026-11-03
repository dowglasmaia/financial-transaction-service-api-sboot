package com.dowglasmaia.financialtransaction.transaction.application.port;

import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/** Output port of the ledger. */
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    /**
     * Statement rows of ONE account: only the given status (the statement asks for COMPLETED, so pending/failed Pix
     * reservations are not part of it) and only the caller's own rows (spec BE-04).
     */
    List<Transaction> findByStatusAndAccountIdAndDateHourBetween(TransactionStatus status, String accountId,
                                                                 OffsetDateTime initialDate, OffsetDateTime finalDate);

    /** A transaction by id, but only when it belongs to the given account (foreign ids behave as unknown ones). */
    Optional<Transaction> findByIdAndAccountId(String id, String accountId);

    /** One statement: a Pix reservation changes status without a read-modify-write. */
    void updateStatus(String id, TransactionStatus status);
}
