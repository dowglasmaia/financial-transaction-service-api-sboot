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

    /**
     * The key as it may appear in a log: never the whole value. CPF keeps its two check digits, an e-mail its first letter
     * and domain, a phone its last four digits. A value that is not a usable key is hidden completely.
     */
    public String masked() {
        if (!isValid()) {
            return "***";
        }
        switch (type) {
            case CPF:
                return "***.***.***-" + value.substring(value.length() - 2);
            case EMAIL:
                return value.charAt(0) + "***" + value.substring(value.indexOf('@'));
            default:
                return "*******" + value.substring(value.length() - 4);
        }
    }
}
