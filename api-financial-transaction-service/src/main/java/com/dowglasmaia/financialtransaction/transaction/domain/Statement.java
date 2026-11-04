package com.dowglasmaia.financialtransaction.transaction.domain;

import java.math.BigDecimal;
import java.util.List;

/** Value object: one page of completed entries, the totals of the period, the balance now and the paging. */
public record Statement(List<Transaction> transactions, BigDecimal balance, StatementTotals totals, int page, int size) {

    public static Statement of(List<Transaction> transactions, BigDecimal balance, StatementTotals totals, int page, int size) {
        return new Statement(transactions, balance, totals, page, size);
    }

    public BigDecimal sumIncoming() {
        return totals.sumIncoming();
    }

    public BigDecimal sumOutgoing() {
        return totals.sumOutgoing();
    }

    public long totalElements() {
        return totals.matching();
    }
}
