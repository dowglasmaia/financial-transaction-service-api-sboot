package com.dowglasmaia.financialtransaction.account.domain;

import com.dowglasmaia.financialtransaction.shared.money.Money;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A customer's account. The balance changes only through the repository's atomic {@code tryDebit}/{@code credit}
 * (one SQL statement each), so this object is a snapshot and has no mutators.
 */
public final class Account {

    private final String id;
    private final Agency agency;
    private final User user;
    private final String number;
    private final BigDecimal balance;

    private Account(String id, Agency agency, User user, String number, BigDecimal balance) {
        this.id = id;
        this.agency = agency;
        this.user = user;
        this.number = number;
        this.balance = balance;
    }

    public static Account create(Agency agency, User user, String number, BigDecimal balance) {
        Objects.requireNonNull(agency, "agency");
        Objects.requireNonNull(user, "user");
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        Money.balance(balance); // invariant: a new account starts with a balance that is zero or more
        return new Account(null, agency, user, number, balance);
    }

    public static Account restore(String id, Agency agency, User user, String number, BigDecimal balance) {
        return new Account(id, agency, user, number, balance);
    }

    public String getId() {
        return id;
    }

    public Agency getAgency() {
        return agency;
    }

    public User getUser() {
        return user;
    }

    public String getNumber() {
        return number;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
