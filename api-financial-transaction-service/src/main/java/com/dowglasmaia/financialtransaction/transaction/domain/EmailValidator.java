package com.dowglasmaia.financialtransaction.transaction.domain;

import java.util.regex.Pattern;

/**
 * E-mail rule for a Pix key (SL-06, F-11): at most 77 characters (the Pix limit); a local part of letters and digits
 * separated by single {@code . _ % + -}; a domain of labels without leading or trailing hyphen; a letters-only top-level
 * domain of 2 to 24 characters. No whitespace, no empty labels, no consecutive dots.
 */
public final class EmailValidator {

    private static final int MAX_LENGTH = 77;
    private static final Pattern EMAIL = Pattern.compile(
          "^[A-Za-z0-9]+([._%+-][A-Za-z0-9]+)*@([A-Za-z0-9]+(-+[A-Za-z0-9]+)*\\.)+[A-Za-z]{2,24}$");

    private EmailValidator() {
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.length() <= MAX_LENGTH && EMAIL.matcher(email).matches();
    }
}
