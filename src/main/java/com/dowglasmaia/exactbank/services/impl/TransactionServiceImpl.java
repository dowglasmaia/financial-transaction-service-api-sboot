package com.dowglasmaia.exactbank.services.impl;

import com.dowglasmaia.exactbank.entity.Transaction;
import com.dowglasmaia.exactbank.entity.TransactionTypeEnum;
import com.dowglasmaia.exactbank.exeptions.UnprocessableEntityExeption;
import com.dowglasmaia.exactbank.repository.TransactionRepository;
import com.dowglasmaia.exactbank.services.TransactionService;
import com.dowglasmaia.provider.model.DepositRequestDTO;
import com.dowglasmaia.provider.model.MobileRechargeRequestDTO;
import com.dowglasmaia.provider.model.PixRequestDTO;
import com.dowglasmaia.provider.model.WithdrawRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;


    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository){
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void save(Object transaction, String accountId, BigDecimal amount, String agencyId){
        var transactionEntity = Transaction.builder()
              .transactionType(getTransactionType(transaction))
              .accountId(accountId)
              .amount(amount)
              .agencyId(agencyId)
              .dateHour(OffsetDateTime.now())
              .build();

        transactionRepository.save(transactionEntity);
    }




    private TransactionTypeEnum getTransactionType(Object object){
        if (object instanceof WithdrawRequestDTO)
            return TransactionTypeEnum.WITHDRAW;

        if (object instanceof DepositRequestDTO)
            return TransactionTypeEnum.DEPOSIT;

        if (object instanceof PixRequestDTO)
            return TransactionTypeEnum.PIX;

        if (object instanceof MobileRechargeRequestDTO)
            return TransactionTypeEnum.PHONE_RECHARGE;

        throw new UnprocessableEntityExeption("Type of transaction is not valid", HttpStatus.UNPROCESSABLE_ENTITY);
    }

}
