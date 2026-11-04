package com.dowglasmaia.financialtransaction.transaction.domain;

/** Value object: a Pix key and its type. Valid when the value satisfies the rule of its type. */
public record PixKey(PixKeyType type, String value) {

    public boolean isValid() {
        switch (type) {
            case CPF:
                return CPFValidator.isCpf(value);
            case EMAIL:
                return EmailValidator.isValidEmail(value);
            case PHONE:
                return PhoneValidator.isValidPhoneNumber(value);
            default:
                return false;
        }
    }

    /** Placeholder until the masking is implemented (SL-06 red run). */
    public String masked() {
        return "";
    }
}
