package com.dowglasmaia.financialtransaction.transaction.domain;

import java.math.BigDecimal;

/** Value object: a mobile top-up request (the carrier is the contract value, for example "VIVO"). */
public record MobileRecharge(Integer areaCode, Integer phoneNumber, BigDecimal amount, String carrier) {

    /** The phone as the rule validates it: the area code followed by the number, digits only. */
    public String fullPhoneNumber() {
        return String.valueOf(areaCode) + phoneNumber;
    }

    public boolean hasValidPhone() {
        return PhoneValidator.isValidPhoneNumber(fullPhoneNumber());
    }
}
