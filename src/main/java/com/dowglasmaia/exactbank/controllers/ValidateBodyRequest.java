package com.dowglasmaia.exactbank.controllers;

import com.dowglasmaia.provider.model.PixRequestDTO;

import static java.util.Objects.nonNull;

public class ValidateBodyRequest {

    public static void validatePixRequestDTO(PixRequestDTO body){
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

}
