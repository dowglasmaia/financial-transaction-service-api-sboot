package com.dowglasmaia.exactbank.mock;

import com.dowglasmaia.provider.model.PixRequestDTO;

import java.math.BigDecimal;

public class MockBuild {

    public static PixRequestDTO pixRequestBuild(){
        PixRequestDTO request = new PixRequestDTO();
        request.setKeyType(PixRequestDTO.KeyTypeEnum.CPF);
        request.setPixKey("09413184003");
        request.setTransactionAmount(BigDecimal.valueOf(500.00));

        return request;
    }
}
