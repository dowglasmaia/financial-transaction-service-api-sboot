package com.dowglasmaia.financialtransaction.transaction.domain;

/** Ledger row state: PENDING while a Pix is reserved but not confirmed, FAILED when it was compensated. */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED
}
