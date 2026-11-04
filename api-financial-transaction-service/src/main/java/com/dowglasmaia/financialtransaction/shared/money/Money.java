package com.dowglasmaia.financialtransaction.shared.money;

import java.math.BigDecimal;

/**
 * Money of the account (Brazilian reais): never negative and with at most two decimal places. More places are REJECTED, not
 * rounded (owner decision, BE-08): a customer who types 10.005 is told, and nothing is silently changed.
 */
public record Money(BigDecimal value) {

    static final String TOO_PRECISE = "Amount must have at most 2 decimal places";

    public Money {
        if (value == null) {
            throw new IllegalArgumentException("Balance is required");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(TOO_PRECISE);
        }
    }

    /** A request amount: required, greater than zero ({@code message} says it otherwise) and with at most two decimal places. */
    public static Money requirePositive(BigDecimal amount, String message) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(message);
        }
        return new Money(amount);
    }

    /** An account balance: zero or more. */
    public static Money balance(BigDecimal value) {
        return new Money(value);
    }
}
