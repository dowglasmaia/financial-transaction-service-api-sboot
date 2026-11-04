package com.dowglasmaia.financialtransaction.transaction.api;

import com.dowglasmaia.provider.model.DepositRequestDTO;
import com.dowglasmaia.provider.model.MobileRechargeRequestDTO;
import com.dowglasmaia.provider.model.PixRequestDTO;
import com.dowglasmaia.provider.model.WithdrawRequestDTO;

import com.dowglasmaia.financialtransaction.shared.money.Money;

/**
 * Shape checks of the four money POST bodies, done once here (SL-03): a null body or a missing, blank or non-positive
 * field is an {@link IllegalArgumentException} (the advice answers 422), never a NullPointerException. The operation
 * specific rules (Pix key check digits, recharge phone and carrier) are not shape checks and live in the use cases.
 */
public class ValidateBodyRequest {

    private static final String AMOUNT = "Amount must be positive and non-null";

    public static void validatorPixRequest(PixRequestDTO body){
        required(body);
        if (isBlank(body.getPixKey())) {
            throw new IllegalArgumentException("Pix key is required");
        }
        Money.requirePositive(body.getTransactionAmount(), "Transaction amount must be positive and non-null");
        if (body.getKeyType() == null) {
            throw new IllegalArgumentException("Key type is required");
        }
    }

    public static void validatorDepositRequest(DepositRequestDTO body){
        required(body);
        if (isBlank(body.getAccount())) {
            throw new IllegalArgumentException("Account is required");
        }
        Money.requirePositive(body.getAmount(), AMOUNT);
        if (isBlank(body.getAgency())) {
            throw new IllegalArgumentException("Agency is required");
        }
    }

    public static void validateMobileRechargeRequest(MobileRechargeRequestDTO body){
        required(body);
        if (body.getPhoneNumber() == null) {
            throw new IllegalArgumentException("Phone number is required");
        }
        Money.requirePositive(body.getAmount(), AMOUNT);
        if (body.getAreaCode() == null) {
            throw new IllegalArgumentException("Area code is required");
        }
        if (body.getCarrier() == null) {
            throw new IllegalArgumentException("Invalid carrier");
        }
    }

    public static void validateWithdrawRequest(WithdrawRequestDTO body){
        required(body);
        Money.requirePositive(body.getAmount(), AMOUNT);
    }

    private static void required(Object body){
        if (body == null) {
            throw new IllegalArgumentException("Request body is required");
        }
    }

    private static boolean isBlank(String value){
        return value == null || value.isBlank();
    }


}
