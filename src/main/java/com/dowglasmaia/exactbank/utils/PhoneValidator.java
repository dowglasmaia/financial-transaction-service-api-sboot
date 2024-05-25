package com.dowglasmaia.exactbank.utils;

public class PhoneValidator {
    private static final String PHONE_NUMBER_PATTERN = "^[1-9]{2}9[0-9]{8}$";

    public static boolean isValidPhoneNumber(String phoneNumber){
        if ( phoneNumber == null) {
            return false;
        }
        return phoneNumber.matches(PHONE_NUMBER_PATTERN);
    }
}
