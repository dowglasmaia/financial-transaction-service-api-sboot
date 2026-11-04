package com.dowglasmaia.financialtransaction.transaction.domain;

import java.util.Arrays;
import java.util.List;

public enum TransactionType {
    PIX,
    PHONE_RECHARGE,
    WITHDRAW,
    DEPOSIT;

    /** Statement classification (BR-52): only a deposit is money coming in. */
    public boolean isIncoming() {
        return this == DEPOSIT;
    }

    /** Pix, mobile recharge and ATM withdrawal are money going out. */
    public boolean isOutgoing() {
        return !isIncoming();
    }

    public static List<TransactionType> incoming() {
        return Arrays.stream(values()).filter(TransactionType::isIncoming).toList();
    }

    public static List<TransactionType> outgoing() {
        return Arrays.stream(values()).filter(TransactionType::isOutgoing).toList();
    }
}
