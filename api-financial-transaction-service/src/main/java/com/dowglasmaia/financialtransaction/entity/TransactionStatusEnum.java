package com.dowglasmaia.financialtransaction.entity;

/** Ledger row state: PENDING while a Pix is reserved but not confirmed, FAILED when it was compensated. */
public enum TransactionStatusEnum {
    PENDING,
    COMPLETED,
    FAILED
}
