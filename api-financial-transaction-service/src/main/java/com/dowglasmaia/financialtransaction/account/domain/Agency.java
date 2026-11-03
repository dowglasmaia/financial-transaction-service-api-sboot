package com.dowglasmaia.financialtransaction.account.domain;

/** A bank agency. */
public final class Agency {

    private final String id;
    private final String number;

    private Agency(String id, String number) {
        this.id = id;
        this.number = number;
    }

    public static Agency create(String number) {
        return new Agency(null, number);
    }

    public static Agency restore(String id, String number) {
        return new Agency(id, number);
    }

    public String getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }
}
