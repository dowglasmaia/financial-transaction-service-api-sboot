package com.dowglasmaia.financialtransaction.transaction.domain;

/** The kinds of Pix key. The label is the lower-case value of the API contract (it appears in the error message). */
public enum PixKeyType {
    PHONE("phone"),
    EMAIL("email"),
    CPF("cpf");

    private final String label;

    PixKeyType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
