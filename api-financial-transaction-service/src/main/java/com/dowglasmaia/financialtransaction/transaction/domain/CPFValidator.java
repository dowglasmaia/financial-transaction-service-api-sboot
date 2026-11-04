package com.dowglasmaia.financialtransaction.transaction.domain;

import java.util.regex.Pattern;

/**
 * CPF rule (SL-06, F-11): exactly 11 digits, plain or masked (999.999.999-99), not all the same digit, and both check
 * digits right. Anything else is simply "not a CPF": the validator never throws.
 */
public final class CPFValidator {

    private static final Pattern PLAIN = Pattern.compile("^\\d{11}$");
    private static final Pattern MASKED = Pattern.compile("^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$");

    private CPFValidator() {
    }

    public static boolean isCpf(String cpf) {
        if (cpf == null || !(PLAIN.matcher(cpf).matches() || MASKED.matcher(cpf).matches())) {
            return false;
        }
        String digits = cpf.replace(".", "").replace("-", "");
        if (digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(digits, 9) == digits.charAt(9) - '0' && checkDigit(digits, 10) == digits.charAt(10) - '0';
    }

    /** Check digit over the first {@code length} digits (weights length+1 down to 2). */
    private static int checkDigit(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (length + 1 - i);
        }
        int rest = sum % 11;
        return rest < 2 ? 0 : 11 - rest;
    }
}
