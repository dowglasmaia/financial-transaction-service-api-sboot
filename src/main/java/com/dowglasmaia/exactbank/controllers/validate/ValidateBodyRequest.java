package com.dowglasmaia.exactbank.controllers.validate;

import com.dowglasmaia.provider.model.DepositRequestDTO;
import com.dowglasmaia.provider.model.PixRequestDTO;

import static java.util.Objects.nonNull;

public class ValidateBodyRequest {

    public static void validatorPixRequest(PixRequestDTO body){
        if (nonNull(body) && body.getPixKey() == null || body.getPixKey().isEmpty()) {
            throw new IllegalArgumentException("Pix key is required");
        }
        if (nonNull(body) && body.getTransactionAmount() == null || body.getTransactionAmount() <= 0) {
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
        if (nonNull(body) && body.getAmount() == null || body.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be positive and non-null");
        }
        if (nonNull(body) && body.getAgency() == null || body.getAgency().isEmpty()) {
            throw new IllegalArgumentException("Agency is required");
        }

    }

}
