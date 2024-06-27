package com.dowglasmaia.financialtransaction.controllers.validate;

import com.dowglasmaia.provider.model.DepositRequestDTO;
import com.dowglasmaia.provider.model.MobileRechargeRequestDTO;
import com.dowglasmaia.provider.model.PixRequestDTO;
import com.dowglasmaia.provider.model.WithdrawRequestDTO;

import java.math.BigDecimal;
import java.util.EnumSet;

import static java.util.Objects.nonNull;

public class ValidateBodyRequest {

    public static void validatorPixRequest(PixRequestDTO body){
        if (nonNull(body) && body.getPixKey() == null || body.getPixKey().isEmpty()) {
            throw new IllegalArgumentException("Pix key is required");
        }
        if (nonNull(body) && body.getTransactionAmount() == null || body.getTransactionAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive and non-null");
        }
        if (nonNull(body) && body.getKeyType() == null) {
            throw new IllegalArgumentException("Key type is required");
        }
    }

    public static void validatorDepositRequest(DepositRequestDTO body){
        if (nonNull(body) && body.getAccount() == null || body.getAccount().isEmpty()) {
            throw new IllegalArgumentException("Account is required");
        }
        if (nonNull(body) && body.getAmount() == null || body.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive and non-null");
        }
        if (nonNull(body) && body.getAgency() == null || body.getAgency().isEmpty()) {
            throw new IllegalArgumentException("Agency is required");
        }
    }

    public static void validateMobileRechargeRequest(MobileRechargeRequestDTO body){
        if (body == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (body.getPhoneNumber() == null) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (body.getAmount() == null || body.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive and non-null");
        }
        if (body.getAreaCode() == null) {
            throw new IllegalArgumentException("Area code is required");
        }
        if (body.getCarrier() == null || !EnumSet.allOf(MobileRechargeRequestDTO.CarrierEnum.class).contains(body.getCarrier())) {
            throw new IllegalArgumentException("Invalid carrier");
        }
    }

    public static void validateWithdrawRequest(WithdrawRequestDTO body){
        if (body.getAmount() == null || body.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive and non-null");
        }
    }

}
