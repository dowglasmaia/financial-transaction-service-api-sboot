package com.dowglasmaia.exactbank.entity;

public enum TransactionTypeEnum {

    PIX("Pix"),
    PHONE_RECHARGE("Cell Phone Recharge"),
    WITHDRAW("Withdraw"),
    DEPOSIT("Deposit");

    private String value;

    TransactionTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}