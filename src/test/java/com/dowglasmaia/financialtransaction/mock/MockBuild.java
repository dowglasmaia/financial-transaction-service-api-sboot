package com.dowglasmaia.financialtransaction.mock;

import com.dowglasmaia.financialtransaction.entity.Account;
import com.dowglasmaia.financialtransaction.entity.Agency;
import com.dowglasmaia.financialtransaction.entity.User;
import com.dowglasmaia.provider.model.PixRequestDTO;
import com.dowglasmaia.provider.model.TransactionResponseDTO;
import com.dowglasmaia.provider.model.WithdrawRequestDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class MockBuild {

    public static PixRequestDTO pixRequestBuild(){
        PixRequestDTO request = new PixRequestDTO();
        request.setKeyType(PixRequestDTO.KeyTypeEnum.CPF);
        request.setPixKey("09413184003");
        request.setTransactionAmount(BigDecimal.valueOf(500.00));

        return request;
    }

    public static TransactionResponseDTO transactionResponseBuild(){
        TransactionResponseDTO response = new TransactionResponseDTO();

        response.setDateHour(OffsetDateTime.now());
        response.setAmount(BigDecimal.valueOf(5000.00));
        response.setTransactionType("PIX");
        return response;
    }

    public static Account userAccountBuild(){
        return Account.builder()
              .user(userBuild())
              .balance(BigDecimal.valueOf(500))
              .number("")
              .id("")
              .agency(agencyBuild())
              .build();

    }

    public static Agency agencyBuild(){
        return Agency.builder()
              .id(String.valueOf(UUID.randomUUID()))
              .number("0220")
              .build();
    }

    public static User userBuild(){
        return User.builder()
              .name("Dowglas")
              .CPF("09413184003")
              .build();
    }

    public static WithdrawRequestDTO withdrawRequestBuild(String accountNumber, String agencyNumber , BigDecimal amount){
        WithdrawRequestDTO request = new WithdrawRequestDTO();
        request.setAgency(agencyNumber);
        request.setNumberAccount(accountNumber);
        request.setAmount(amount);

        return  request;
    }
}

