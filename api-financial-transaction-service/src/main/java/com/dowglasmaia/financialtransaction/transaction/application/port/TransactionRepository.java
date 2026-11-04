package com.dowglasmaia.financialtransaction.transaction.application.port;

import com.dowglasmaia.financialtransaction.transaction.domain.DailyTotal;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementPeriod;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementQuery;
import com.dowglasmaia.financialtransaction.transaction.domain.StatementTotals;
import com.dowglasmaia.financialtransaction.transaction.domain.Transaction;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionStatus;
import com.dowglasmaia.financialtransaction.transaction.domain.TransactionType;
import java.util.List;
import java.util.Optional;

/** Output port of the ledger. */
public interface TransactionRepository {

    Transaction save(Transaction transaction);

    /**
     * One page of statement rows of ONE account (spec BE-04): only COMPLETED entries (pending/failed Pix reservations
     * are not part of it), inside the half-open period, filtered, sorted and paged as the query says.
     */
    List<Transaction> findStatementRows(String accountId, StatementQuery query);

    /**
     * Sums (whole period, any type) and the count of rows that match the optional type filter, in ONE aggregate query.
     * A null type means no filter.
     */
    StatementTotals totals(String accountId, StatementPeriod period, TransactionType type);

    /** Incoming/outgoing per Sao Paulo day for the period; days without entries are absent (the domain fills them). */
    List<DailyTotal> dailyTotals(String accountId, StatementPeriod period);

    /** A transaction by id, but only when it belongs to the given account (foreign ids behave as unknown ones). */
    Optional<Transaction> findByIdAndAccountId(String id, String accountId);

    /** One statement: a Pix reservation changes status without a read-modify-write. */
    void updateStatus(String id, TransactionStatus status);
}
